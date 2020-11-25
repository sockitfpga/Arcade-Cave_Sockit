/*
 *    __   __     __  __     __         __
 *   /\ "-.\ \   /\ \/\ \   /\ \       /\ \
 *   \ \ \-.  \  \ \ \_\ \  \ \ \____  \ \ \____
 *    \ \_\\"\_\  \ \_____\  \ \_____\  \ \_____\
 *     \/_/ \/_/   \/_____/   \/_____/   \/_____/
 *    ______     ______       __     ______     ______     ______
 *   /\  __ \   /\  == \     /\ \   /\  ___\   /\  ___\   /\__  _\
 *   \ \ \/\ \  \ \  __<    _\_\ \  \ \  __\   \ \ \____  \/_/\ \/
 *    \ \_____\  \ \_____\ /\_____\  \ \_____\  \ \_____\    \ \_\
 *     \/_____/   \/_____/ \/_____/   \/_____/   \/_____/     \/_/
 *
 *  https://joshbassett.info
 *  https://twitter.com/nullobject
 *  https://github.com/nullobject
 *
 *  Copyright (c) 2020 Josh Bassett
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package axon.snd

import chisel3._
import chisel3.util._

/**
 * A channel register.
 *
 * It contains the parameters that describe the behavior of a channel. For example, whether a
 * channel is playing, looping, volume, pan, etc.
 *
 * Channel registers are stored in the main register file, as specified in the YMZ280B datasheet.
 */
class ChannelReg extends Bundle {
  /** Playback pitch (172Hz to 44100Hz in 256 steps) */
  val pitch = UInt(8.W)
  /** Flags */
  val flags = new Bundle {
    /** Key on */
    val keyOn = Bool()
    /** Quantization mode */
    val quantizationMode = UInt(2.W)
    /** Loop */
    val loop = Bool()
  }
  /** Playback level (256 steps) */
  val level = UInt(8.W)
  /** Panning (16 steps) */
  val pan = UInt(4.W)
  /** Start address */
  val startAddr = UInt(ChannelReg.ADDR_WIDTH.W)
  /** Loop start address */
  val loopStartAddr = UInt(ChannelReg.ADDR_WIDTH.W)
  /** Loop end address */
  val loopEndAddr = UInt(ChannelReg.ADDR_WIDTH.W)
  /** End address */
  val endAddr = UInt(ChannelReg.ADDR_WIDTH.W)
}

object ChannelReg {
  /** The width of the address bus */
  val ADDR_WIDTH = 24

  /**
   * Decodes a channel register from the given register file.
   *
   * @param registerFile The register file.
   * @param n The channel number.
   */
  def fromRegisterFile(registerFile: Vec[UInt])(n: Int): ChannelReg = {
    val offset = n*4
    Cat(
      registerFile(offset+0x00),       // pitch
      registerFile(offset+0x01)(7, 4), // flags
      registerFile(offset+0x02),       // level
      registerFile(offset+0x03)(3, 0), // pan
      registerFile(offset+0x20),       // start address
      registerFile(offset+0x40),
      registerFile(offset+0x60),
      registerFile(offset+0x21),       // loop start address
      registerFile(offset+0x41),
      registerFile(offset+0x61),
      registerFile(offset+0x22),       // loop end address
      registerFile(offset+0x42),
      registerFile(offset+0x62),
      registerFile(offset+0x23),       // end address
      registerFile(offset+0x43),
      registerFile(offset+0x63)
    ).asTypeOf(new ChannelReg)
  }
}
