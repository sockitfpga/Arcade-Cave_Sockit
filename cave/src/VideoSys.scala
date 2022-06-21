/*
 *   __   __     __  __     __         __
 *  /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *  \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *   \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *    \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *   ______     ______       __     ______     ______     ______
 *  /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *  \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *   \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *    \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 * https://joshbassett.info
 * https://twitter.com/nullobject
 * https://github.com/nullobject
 *
 * Copyright (c) 2022 Josh Bassett
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cave

import arcadia._
import arcadia.gfx._
import arcadia.mem.RegisterFile
import arcadia.mister._
import cave.gfx._
import chisel3._
import chisel3.util._

/**
 * The video subsystem generates video timing signals for both original and compatibility (60Hz)
 * video modes.
 */
class VideoSys extends Module {
  val io = IO(new Bundle {
    /** Video clock domain */
    val videoClock = Input(Clock())
    /** Video reset */
    val videoReset = Input(Bool())
    /** IOCTL port */
    val ioctl = IOCTL()
    /** Options port */
    val options = OptionsIO()
    /** Video registers */
    val regs = Output(new VideoRegs)
    /** Video port */
    val video = VideoIO()
  })

  // Asserted when the video registers have been written to the IOCTL
  val videoDownloaded = Util.falling(io.ioctl.download) && io.ioctl.index === IOCTL.VIDEO_INDEX.U

  // Connect IOCTL to video register file
  val videoRegs = Module(new RegisterFile(IOCTL.DATA_WIDTH, VideoSys.VIDEO_REGS_COUNT))
  videoRegs.io.mem <> io.ioctl.video
    .mapAddr { a => (a >> 1).asUInt } // convert from byte address
    .mapData(Util.swapEndianness) // swap bytes
    .asReadWriteMemIO

  // Latch decoded video registers after they have been downloaded
  io.regs := RegEnable(VideoRegs.decode(videoRegs.io.regs), VideoSys.DEFAULT_REGS, videoDownloaded)

  // Video timing runs in the video clock domain
  val timing = withClockAndReset(io.videoClock, io.videoReset) {
    // Original video timing
    val originalVideoTiming = Module(new VideoTiming(Config.originalVideoTimingConfig))
    originalVideoTiming.io.display := io.regs.display
    originalVideoTiming.io.frontPorch := io.regs.frontPorch
    originalVideoTiming.io.retrace := io.regs.retrace

    // Compatibility video timing
    val compatibilityVideoTiming = Module(new VideoTiming(Config.compatibilityVideoTimingConfig))
    compatibilityVideoTiming.io.display := UVec2(320.U, 240.U)
    compatibilityVideoTiming.io.frontPorch := UVec2(30.U, 12.U)
    compatibilityVideoTiming.io.retrace := UVec2(20.U, 2.U)

    // Changing the CRT offset during the display region can momentarily alter the screen
    // dimensions, which may cause issues with other modules. If we latch the offset during a
    // vertical sync, then we can avoid causing any problems.
    originalVideoTiming.io.offset := RegEnable(io.options.offset, originalVideoTiming.io.timing.vSync)
    compatibilityVideoTiming.io.offset := RegEnable(io.options.offset, compatibilityVideoTiming.io.timing.vSync)

    // The compatibility option is latched during a vertical blank to avoid any sudden changes in
    // the video timing.
    val latchReg = RegEnable(io.options.compatibility, originalVideoTiming.io.timing.vBlank && compatibilityVideoTiming.io.timing.vBlank)

    // Select original or compatibility video timing
    val timing = Mux(latchReg, compatibilityVideoTiming.io.timing, originalVideoTiming.io.timing)

    // Register all video timing signals
    RegNext(timing)
  }

  val video = Wire(new VideoIO)
  video.clock := io.videoClock
  video.reset := io.videoReset
  video.clockEnable := timing.clockEnable
  video.displayEnable := timing.displayEnable
  video.changeMode := videoDownloaded || (io.options.compatibility ^ RegNext(io.options.compatibility))
  video.pos := timing.pos
  video.hSync := timing.hSync
  video.vSync := timing.vSync
  video.hBlank := timing.hBlank
  video.vBlank := timing.vBlank

  // Outputs
  io.video <> video
}

object VideoSys {
  /** The number of video registers */
  val VIDEO_REGS_COUNT = 8

  /** Default video registers */
  val DEFAULT_REGS = VideoRegs(
    width = Config.SCREEN_WIDTH,
    height = Config.SCREEN_HEIGHT,
    hFrontPorch = 36,
    vFrontPorch = 12,
    hRetrace = 20,
    vRetrace = 2
  )
}
