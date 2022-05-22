package hadoukenify

import org.scalajs.dom._
import org.scalajs.dom.html.{ Canvas, Image, TextArea }
import typings.std.HTMLButtonElement
import typings.std.stdStrings.click

import scala.scalajs.js
import scala.scalajs.js.annotation._

@JSExportTopLevel("hadoukenify")
object Main {

  def main(args: Array[String]): Unit = {
    val imageFrames = document.getElementById("image-frames").children.toArray.map(_.asInstanceOf[Image])

    val canvas = document.createElement("canvas").asInstanceOf[Canvas]
    val ctx    = canvas.getContext("2d", js.Dynamic.literal(alpha = false)).asInstanceOf[CanvasRenderingContext2D]

    val generateButton       = document.getElementById("generate-button").asInstanceOf[HTMLButtonElement]
    val codeInputTextarea    = document.getElementById("code-input").asInstanceOf[TextArea]
    val generatedResultImage = document.getElementById("generated-result").asInstanceOf[HTMLImageElement]
    val downloadButton       = document.getElementById("download-button").asInstanceOf[HTMLLinkElement]
    val loadingMessage       = document.getElementById("loading-message")

    // TODO: Is there a better way than to use `addEventListener_click` to disambiguate the `addEventListener` overloads?
    generateButton.addEventListener_click(
      click,
      { (_: js.Any, _: MouseEvent) =>
        generateButton.disabled = true
        codeInputTextarea.disabled = true
        loadingMessage.classList.remove("hidden")
        generatedResultImage.classList.add("hidden")
        downloadButton.classList.add("hidden")

        val codeLines = CodeLine.parseCode(codeInputTextarea.value)
        val gif       = Renderer.render(codeLines, ctx, imageFrames)

        gif.on(
          "finished",
          { blob =>
            val imageSrc = URL.createObjectURL(blob.asInstanceOf[Blob])
            generatedResultImage.classList.remove("hidden")
            generatedResultImage.src = imageSrc
            downloadButton.classList.remove("hidden")
            downloadButton.href = imageSrc

            generateButton.disabled = false
            codeInputTextarea.disabled = false
            loadingMessage.classList.add("hidden")

            ()
          }
        )

        gif.render()
        ()
      }
    )
  }

}
