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

package cave.gfx

import arcadia.UVec2
import chisel3._

/** A bundle that contains the video registers. */
class VideoRegs extends Bundle {
  /** Display region */
  val display = UVec2(9.W)
  /** Front porch region */
  val frontPorch = UVec2(9.W)
  /** Retrace region */
  val retrace = UVec2(9.W)
}

object VideoRegs {
  /**
   * Decodes the video registers from the given data.
   *
   * {{{
   * word   bits                  description
   * -----+-fedc-ba98-7654-3210-+----------------
   *    0 | ---- ---x xxxx xxxx | display x
   *    1 | ---- ---x xxxx xxxx | display y
   *    2 | ---- ---x xxxx xxxx | front porch x
   *    3 | ---- ---x xxxx xxxx | front porch y
   *    4 | ---- ---x xxxx xxxx | retrace x
   *    5 | ---- ---x xxxx xxxx | retrace y
   * }}}
   *
   * @param data The video registers data.
   */
  def decode[T <: Bits](data: Vec[T]): VideoRegs = {
    val regs = Wire(new VideoRegs)
    regs.display.x := data(0)(8, 0)
    regs.display.y := data(1)(8, 0)
    regs.frontPorch.x := data(2)(8, 0)
    regs.frontPorch.y := data(3)(8, 0)
    regs.retrace.x := data(4)(8, 0)
    regs.retrace.y := data(5)(8, 0)
    regs
  }
}
