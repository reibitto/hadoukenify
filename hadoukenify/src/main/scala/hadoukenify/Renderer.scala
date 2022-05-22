package hadoukenify

import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.html.Image
import typings.gifJs.mod.{ ^, AddFrameOptions, GIF }

object Renderer {
  val backgroundColor = "#fcfefc"
  val textColor       = "#000"
  val minHeight       = 300

  def render(codeLines: Array[CodeLine], ctx: CanvasRenderingContext2D, imageFrames: Array[Image]): GIF = {
    val canvas = ctx.canvas

    // TODO: What's with this `^` type? Might be something ScalablyTyped did. Is there a way to initialize the `GIF`
    // trait directly with an anonymous class without Scala.js complaining?
    val gif = new ^() {}

    val longestLineWidth = codeLines.map(_.text.length).maxOption.getOrElse(0)

    // TODO: Make the auto-sizing better. Consider the number of lines too instead of just the max line length.
    val fontSize = longestLineWidth match {
      case w if w > 120 => 12
      case w if w > 110 => 14
      case w if w > 100 => 15
      case w if w > 80  => 16
      case w if w > 60  => 18
      case w if w > 50  => 20
      case _            => 24
    }

    val lineHeight = (fontSize * 1.2).toInt

    ctx.font = s"${fontSize}px Monospace"
    val textSize = ctx.measureText(" ").width

    val codeWidth          = longestLineWidth * textSize
    val startLineX         = 190
    val optimalImageHeight = (lineHeight * codeLines.length + lineHeight * 0.6).toInt
    val targetImageWidth   = (startLineX + Math.ceil(codeWidth) + textSize).toInt
    val targetImageHeight  = Math.max(optimalImageHeight, minHeight)
    val extraHeight        = Math.max(0, targetImageHeight - optimalImageHeight - lineHeight * 1.5)
    val globalOffsetY      = extraHeight / 2

    val largestIndex = codeLines.map(_.indent).maxOption.getOrElse(0)

    val linesWithLargestIndent = codeLines.zipWithIndex.filter { case (codeLine, _) =>
      codeLine.indent == largestIndex
    }.map { case (_, lineIndex) =>
      lineIndex
    }

    val centerLineIndex = Math.round(linesWithLargestIndent.sum / linesWithLargestIndent.length.toDouble).toInt
    val ryuY            = lineHeight * centerLineIndex + lineHeight / 2 - 105
    val ryuWidth        = 550.0

    canvas.width = targetImageWidth
    canvas.height = targetImageHeight

    ctx.fillStyle = backgroundColor
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    val startupFrameCount = 21

    // TODO: A lot of this is duplicate code. Extract the common parts into separate functions.
    (0 until startupFrameCount).foreach { frame =>
      ctx.fillStyle = backgroundColor
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      val currentImage = imageFrames(Math.min(frame, imageFrames.length - 1))

      ctx.drawImage(
        currentImage,
        -55,
        ryuY + globalOffsetY,
        ryuWidth,
        ryuWidth * currentImage.naturalHeight / currentImage.naturalWidth
      )

      ctx.font = s"${fontSize}px Monospace"
      ctx.fillStyle = textColor

      codeLines.zipWithIndex.foreach { case (codeLine, j) =>
        ctx.fillText(codeLine.unintendedText, startLineX, lineHeight + j * lineHeight + globalOffsetY)
      }

      val options = AddFrameOptions()
      options.delay = 100

      gif.addFrame(ctx.getImageData(0, 0, targetImageWidth, targetImageHeight), options)
    }

    val pushFrameCount    = 14
    val activeFrameCount  = 14
    val recoverFrameCount = 8

    (0 until (activeFrameCount + recoverFrameCount) * 3).foreach { frame =>
      ctx.fillStyle = backgroundColor
      ctx.fillRect(0, 0, canvas.width, canvas.height)

      val currentImage =
        imageFrames(Math.min(startupFrameCount + frame / 3, imageFrames.length - 1))

      ctx.drawImage(
        currentImage,
        -55,
        ryuY + globalOffsetY,
        ryuWidth,
        ryuWidth * currentImage.naturalHeight / currentImage.naturalWidth
      )

      ctx.font = s"${fontSize}px Monospace"
      ctx.fillStyle = textColor

      codeLines.zipWithIndex.foreach { case (codeLine, j) =>
        val percentDone = if (frame > pushFrameCount * 3) {
          1.0
        } else {
          val n = frame / (pushFrameCount * 3.0)

          // Easing: elastic out
          val c = Math.PI * 0.67
          n match {
            case 0 => 0
            case 1 => 1
            case _ => Math.pow(2, -10 * n) * Math.sin((n * 10 - 0.75) * c) + 1
          }
        }

        val lineX = startLineX + percentDone * textSize * codeLine.indent

        ctx.fillText(codeLine.unintendedText, lineX, lineHeight + j * lineHeight + globalOffsetY)
      }

      val options = AddFrameOptions()
      options.delay = frame match {
        case f if f == (activeFrameCount + recoverFrameCount) * 3 - 1 => 1500        // Hold pose for a little bit
        case _                                                        => 1000 / 30.0 // 30 FPS
      }

      gif.addFrame(ctx.getImageData(0, 0, targetImageWidth, targetImageHeight), options)
    }

    gif
  }
}
