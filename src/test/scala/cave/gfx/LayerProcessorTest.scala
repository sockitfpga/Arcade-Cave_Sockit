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

import chisel3._
import chiseltest._
import org.scalatest._
import flatspec.AnyFlatSpec
import matchers.should.Matchers

class LayerProcessorTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  it should "load tiles from VRAM" in {
    test(new LayerProcessor(0)) { dut =>
      dut.io.ctrl.regs.tileSize.poke(false)
      dut.io.video.pos.x.poke(0)
      dut.io.ctrl.vram16x16.addr.expect(0x01)
      dut.io.video.pos.x.poke(8)
      dut.io.ctrl.vram16x16.addr.expect(0x02)
      dut.io.video.pos.x.poke(248)
      dut.io.ctrl.vram16x16.addr.expect(0x20)
    }
  }

  it should "load line effects from VRAM" in {
    test(new LayerProcessor(0)) { dut =>
      dut.io.video.hSync.poke(true.B)
      dut.clock.step()
      dut.io.video.hSync.poke(false.B)
      dut.io.ctrl.lineRam.addr.expect(0)
    }
  }
}
