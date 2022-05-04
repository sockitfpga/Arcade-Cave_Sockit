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

import axon._
import axon.gfx._
import axon.mem._
import axon.mem.sdram.{SDRAM, SDRAMIO}
import axon.mister._
import axon.snd._
import axon.types._
import cave.fb._
import cave.types._
import chisel3._
import chisel3.experimental.FlatIO
import chisel3.stage._

/**
 * The top-level module.
 *
 * The main module abstracts the rest of the arcade hardware from MiSTer-specific things (e.g.
 * memory arbiter, frame buffer) that are not part of the original arcade hardware design.
 */
class Main extends Module {
  val io = FlatIO(new Bundle {
    /** Video clock domain */
    val videoClock = Input(Clock())
    /** Video reset */
    val videoReset = Input(Bool())
    /** CPU reset */
    val cpuReset = Input(Bool())
    /** DDR port */
    val ddr = BurstReadWriteMemIO(Config.ddrConfig)
    /** SDRAM control port */
    val sdram = SDRAMIO(Config.sdramConfig)
    /** Options port */
    val options = OptionsIO()
    /** Joystick port */
    val joystick = JoystickIO()
    /** IOCTL port */
    val ioctl = IOCTL()
    /** Frame buffer control port */
    val frameBufferCtrl = FrameBufferCtrlIO(Config.SCREEN_WIDTH, Config.SCREEN_HEIGHT)
    /** Audio port */
    val audio = Output(new Audio(Config.ymzConfig.sampleWidth))
    /** Video port */
    val video = VideoIO()
    /** RGB output */
    val rgb = Output(RGB(Config.RGB_OUTPUT_BPP.W))
    /** LED port */
    val led = mister.LEDIO()
  })

  // The download done register is latched when the ROM download has completed.
  val downloadDoneReg = Util.latchSync(Util.falling(io.ioctl.download))

  // The game configuration register is latched when data is written to the download port (i.e. the
  // game index is set by the MRA file).
  val gameConfigReg = {
    val gameConfig = Reg(GameConfig())
    val latched = RegInit(false.B)
    when(io.ioctl.download && io.ioctl.wr && io.ioctl.index === IOCTL.GAME_INDEX.U) {
      gameConfig := GameConfig(io.ioctl.dout(OptionsIO.GAME_INDEX_WIDTH - 1, 0))
      latched := true.B
    }
    // Default to the game configuration set in the options
    when(Util.falling(io.ioctl.download) && !latched) {
      gameConfig := GameConfig(io.options.gameIndex)
      latched := true.B
    }
    gameConfig
  }

  // DDR controller
  val ddr = Module(new DDR(Config.ddrConfig))
  ddr.io.ddr <> io.ddr

  // SDRAM controller
  val sdram = Module(new SDRAM(Config.sdramConfig))
  sdram.io.sdram <> io.sdram

  // Memory subsystem
  val memSys = Module(new MemSys)
  memSys.io.gameConfig <> gameConfigReg
  memSys.io.ioctl <> io.ioctl
  memSys.io.ddr <> ddr.io.mem
  memSys.io.sdram <> sdram.io.mem

  // Video subsystem
  val videoSys = Module(new VideoSys)
  videoSys.io.videoClock := io.videoClock
  videoSys.io.videoReset := io.videoReset
  videoSys.io.options <> io.options
  videoSys.io.video <> io.video

  // Cave
  val cave = Module(new Cave)
  cave.io.videoClock := io.videoClock
  cave.io.videoReset := io.videoReset
  cave.io.cpuReset := io.cpuReset
  cave.io.gameConfig <> gameConfigReg
  cave.io.options <> io.options
  cave.io.joystick <> io.joystick
  cave.io.progRom <> memSys.io.progRom
  cave.io.soundRom <> memSys.io.soundRom
  cave.io.eeprom <> memSys.io.eeprom
  cave.io.layerTileRom(0) <> ClockDomain.syncronize(io.videoClock, memSys.io.layerTileRom(0))
  cave.io.layerTileRom(1) <> ClockDomain.syncronize(io.videoClock, memSys.io.layerTileRom(1))
  cave.io.layerTileRom(2) <> ClockDomain.syncronize(io.videoClock, memSys.io.layerTileRom(2))
  cave.io.spriteTileRom <> memSys.io.spriteTileRom
  cave.io.audio <> io.audio
  cave.io.video <> videoSys.io.video
  cave.io.rgb <> io.rgb

  // Sprite frame buffer
  val spriteFrameBuffer = Module(new SpriteFrameBuffer)
  spriteFrameBuffer.io.videoClock := io.videoClock
  spriteFrameBuffer.io.videoReset := io.videoReset
  spriteFrameBuffer.io.enable := downloadDoneReg
  spriteFrameBuffer.io.frameStart := cave.io.frameStart
  spriteFrameBuffer.io.frameFinish := cave.io.frameFinish
  spriteFrameBuffer.io.video <> videoSys.io.video
  spriteFrameBuffer.io.gpu.lineBuffer <> cave.io.spriteLineBuffer
  spriteFrameBuffer.io.gpu.frameBuffer <> cave.io.spriteFrameBuffer
  spriteFrameBuffer.io.ddr.lineBuffer <> memSys.io.spriteLineBuffer
  spriteFrameBuffer.io.ddr.frameBuffer <> memSys.io.spriteFrameBuffer

  // System frame buffer
  val systemFrameBuffer = Module(new SystemFrameBuffer)
  systemFrameBuffer.io.videoClock := io.videoClock
  systemFrameBuffer.io.videoReset := io.videoReset
  systemFrameBuffer.io.enable := io.options.rotate // enable only for rotated HDMI output
  systemFrameBuffer.io.lowLat := io.frameBufferCtrl.lowLat
  systemFrameBuffer.io.forceBlank := io.cpuReset
  systemFrameBuffer.io.rotate := io.options.rotate
  systemFrameBuffer.io.video <> videoSys.io.video
  systemFrameBuffer.io.frameBufferCtrl <> io.frameBufferCtrl
  systemFrameBuffer.io.frameBuffer <> cave.io.systemFrameBuffer
  systemFrameBuffer.io.ddr <> memSys.io.systemFrameBuffer

  // System LED outputs
  io.led.power := false.B
  io.led.disk := io.ioctl.waitReq
  io.led.user := io.ioctl.download
}

object Main extends App {
  (new ChiselStage).execute(
    Array("--compiler", "verilog", "--target-dir", "quartus/rtl"),
    Seq(ChiselGeneratorAnnotation(() => new Main))
  )
}
