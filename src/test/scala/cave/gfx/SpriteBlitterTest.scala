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

class SpriteBlitterTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "sprite data"

  it should "request sprite data when the PISO is empty" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.ready.expect(true)
    }
  }

  it should "request sprite data when blitting the last pixel" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x100)
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.pixelData.valid.poke(true)
      dut.clock.step()
      dut.io.config.ready.expect(false)
      dut.clock.step(255)
      dut.io.config.ready.expect(true)
    }
  }

  behavior of "pixel data"

  it should "request pixel data when the PISO is empty" in {
    test(new SpriteBlitter) { dut =>
      dut.io.pixelData.valid.poke(true)
      dut.io.pixelData.ready.expect(true)
    }
  }

  it should "request pixel data when the PISO is almost empty" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x100)
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.pixelData.valid.poke(true)
      dut.clock.step()
      dut.io.pixelData.ready.expect(false)
      dut.clock.step(15)
      dut.io.pixelData.ready.expect(true)
    }
  }

  behavior of "blitting"

  it should "assert the busy signal" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x100)
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.pixelData.valid.poke(true)
      dut.clock.step()
      dut.io.config.valid.poke(false)
      dut.io.busy.expect(false)
      dut.clock.step(2)
      dut.io.busy.expect(true)
      dut.clock.step(256)
      dut.io.busy.expect(false)
    }
  }

  it should "write pixel data to the frame buffer" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(2)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.pos.x.poke(0x1000)
      dut.io.config.bits.sprite.pos.y.poke(0x1000)
      dut.io.config.bits.sprite.zoom.x.poke(0x100)
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.pixelData.valid.poke(true)
      for (n <- 0 to 15) { dut.io.pixelData.bits(n).poke(1) }
      dut.clock.step(3)

      // Pixel 0
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x1410.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x1411.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step(29)

      // Pixel 30
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x142e.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 31
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x142f.U)
      dut.io.frameBuffer.din.expect(1.U)
    }
  }

  it should "handle horizontal flipping" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x100)
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.config.bits.sprite.flipX.poke(true)
      dut.io.pixelData.valid.poke(true)
      for (n <- 0 to 15) { dut.io.pixelData.bits(n).poke(1) }
      dut.clock.step(3)

      // Pixel 0
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0f.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.addr.expect(0x0e.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step(13)

      // Pixel 14
      dut.io.frameBuffer.addr.expect(0x01.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 15
      dut.io.frameBuffer.addr.expect(0x00.U)
      dut.io.frameBuffer.din.expect(1.U)
    }
  }

  it should "handle vertical flipping" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x100)
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.config.bits.sprite.flipY.poke(true)
      dut.io.pixelData.valid.poke(true)
      for (n <- 0 to 15) { dut.io.pixelData.bits(n).poke(1) }
      dut.clock.step(3)

      // Pixel 0
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x12c0.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.addr.expect(0x12c1.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step(13)

      // Pixel 14
      dut.io.frameBuffer.addr.expect(0x12ce.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 15
      dut.io.frameBuffer.addr.expect(0x12cf.U)
      dut.io.frameBuffer.din.expect(1.U)
    }
  }

  behavior of "downscaling"

  it should "write pixel data to the frame buffer" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x80) // 0.5x scaling
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.pixelData.valid.poke(true)
      for (n <- 0 to 15) { dut.io.pixelData.bits(n).poke(1) }
      dut.clock.step(3)

      // Pixel 0
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x00.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x00.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step(13)

      // Pixel 14
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x07.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 15
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x07.U)
      dut.io.frameBuffer.din.expect(1.U)
    }
  }

  it should "handle horizontal flipping" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x80) // 0.5x scaling
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.config.bits.sprite.flipX.poke(true)
      dut.io.pixelData.valid.poke(true)
      for (n <- 0 to 15) { dut.io.pixelData.bits(n).poke(1) }
      dut.clock.step(3)

      // Pixel 0
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x0f.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.addr.expect(0x0e.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step(13)

      // Pixel 14
      dut.io.frameBuffer.addr.expect(0x08.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 15
      dut.io.frameBuffer.addr.expect(0x07.U)
      dut.io.frameBuffer.din.expect(1.U)
    }
  }

  behavior of "upscaling"

  it should "write pixel data to the frame buffer" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x200) // 2x scaling
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.pixelData.valid.poke(true)
      for (n <- 0 to 15) { dut.io.pixelData.bits(n).poke(1) }
      dut.clock.step(3)

      // Pixel 0
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x00.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x02.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step(13)

      // Pixel 14
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x1c.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 15
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0x1e.U)
      dut.io.frameBuffer.din.expect(1.U)
    }
  }

  it should "handle horizontal flipping" in {
    test(new SpriteBlitter) { dut =>
      dut.io.config.valid.poke(true)
      dut.io.config.bits.sprite.cols.poke(1)
      dut.io.config.bits.sprite.rows.poke(1)
      dut.io.config.bits.sprite.zoom.x.poke(0x200) // 2x scaling
      dut.io.config.bits.sprite.zoom.y.poke(0x100)
      dut.io.config.bits.sprite.flipX.poke(true)
      dut.io.pixelData.valid.poke(true)
      for (n <- 0 to 15) { dut.io.pixelData.bits(n).poke(1) }
      dut.clock.step(3)

      // Pixel 0
      dut.io.frameBuffer.wr.expect(true)
      dut.io.frameBuffer.addr.expect(0xf.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 1
      dut.io.frameBuffer.addr.expect(0xd.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step(13)

      // Pixel 14
      dut.io.frameBuffer.addr.expect(0x1f3.U)
      dut.io.frameBuffer.din.expect(1.U)
      dut.clock.step()

      // Pixel 15
      dut.io.frameBuffer.addr.expect(0x1f1.U)
      dut.io.frameBuffer.din.expect(1.U)
    }
  }
}
