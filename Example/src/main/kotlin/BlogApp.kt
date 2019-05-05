package blog

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File

fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Routing) {
        get("/") {
            call.respondText("My Example Blog2", ContentType.Text.Html)
        }

        get("jopa") {
            val path = System.getProperty("user.dir")
            call.respondText("$path", ContentType.Text.Html)
        }

        post("/upload") { _ ->
            // retrieve all multipart data (suspending)
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                // if part is a file (could be form item)
                if (part is PartData.FileItem) {
                    // retrieve file name of upload
                    val name = part.originalFileName!!
                    val path = System.getProperty("user.dir")
                    val file = File("$path/uploads/$name")

                    // use InputStream from part to save file
                    part.streamProvider().use { its ->
                        // copy the stream to the file with buffering
                        file.outputStream().buffered().use {
                            // note that this is blocking
                            its.copyTo(it)
                        }
                    }
                }
                // make sure to dispose of the part after use to prevent leaks
                part.dispose()
            }
        }
    }
}

fun main(args: Array<String>) {
    embeddedServer(Netty, 8080, watchPaths = listOf("BlogAppKt"), module = Application::module).start()
}