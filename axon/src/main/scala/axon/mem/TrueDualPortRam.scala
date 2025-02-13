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

package axon.mem

import chisel3._

/**
 * A dual-port RAM with separate clocks for each port.
 *
 * Port A is read-write, and is clocked with the default clock. Port B is read-only, and is clocked
 * with the [[clockB]] input.
 *
 * @param addrWidthA The width of the port A address bus.
 * @param dataWidthA The width of the port A data bus.
 * @param depthA     The optional memory depth of port A (in words).
 * @param addrWidthB The width of the port B address bus.
 * @param dataWidthB The width of the port B data bus.
 * @param depthB     The optional memory depth of port B (in words).
 * @param maskEnable A boolean value indicating whether byte masking is enabled.
 */
class TrueDualPortRam(addrWidthA: Int,
                      dataWidthA: Int,
                      depthA: Option[Int] = None,
                      addrWidthB: Int,
                      dataWidthB: Int,
                      depthB: Option[Int] = None,
                      maskEnable: Boolean = false) extends Module {
  val io = IO(new Bundle {
    /** Clock B */
    val clockB = Input(Clock())
    /** Read-write port */
    val portA = Flipped(ReadWriteMemIO(addrWidthA, dataWidthA))
    /** Read-only port */
    val portB = Flipped(ReadMemIO(addrWidthB, dataWidthB))
  })

  private val depthA_ = depthA.getOrElse(0)
  private val depthB_ = depthB.getOrElse(0)

  class WrappedTrueDualPortRam extends BlackBox(Map(
    "ADDR_WIDTH_A" -> addrWidthA,
    "DATA_WIDTH_A" -> dataWidthA,
    "DEPTH_A" -> depthA_,
    "ADDR_WIDTH_B" -> addrWidthB,
    "DATA_WIDTH_B" -> dataWidthB,
    "DEPTH_B" -> depthB_,
    "MASK_ENABLE" -> (if (maskEnable) "TRUE" else "FALSE"),
  )) {
    val io = IO(new Bundle {
      // port A
      val clk_a = Input(Clock())
      val rd_a = Input(Bool())
      val wr_a = Input(Bool())
      val addr_a = Input(UInt(addrWidthA.W))
      val mask_a = Input(Bits((dataWidthA / 8).W))
      val din_a = Input(Bits(dataWidthA.W))
      val dout_a = Output(Bits(dataWidthA.W))

      // port B
      val clk_b = Input(Clock())
      val rd_b = Input(Bool())
      val addr_b = Input(UInt(addrWidthB.W))
      val dout_b = Output(Bits(dataWidthB.W))
    })

    override def desiredName = "true_dual_port_ram"
  }

  val ram = Module(new WrappedTrueDualPortRam)
  ram.io.clk_a := clock
  ram.io.rd_a := io.portA.rd
  ram.io.wr_a := io.portA.wr
  ram.io.addr_a := io.portA.addr
  ram.io.mask_a := io.portA.mask
  ram.io.din_a := io.portA.din
  io.portA.dout := ram.io.dout_a
  ram.io.clk_b := io.clockB
  ram.io.rd_b := io.portB.rd
  ram.io.addr_b := io.portB.addr
  io.portB.dout := ram.io.dout_b
}
