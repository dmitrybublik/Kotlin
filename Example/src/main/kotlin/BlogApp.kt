package blog

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.request.receiveText
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.sessions.*
import java.io.File

data class RedSession(val name: String)

fun Application.module() {
    install(Sessions) {
        cookie<RedSession>("SESSION", storage = SessionStorageMemory()) {
            cookie.path = "/"
        }
    }
    install(DefaultHeaders)
    install(CallLogging)
    install(Routing) {
        get("/") {
            val path = System.getProperty("user.dir")
            call.respondText("$path", ContentType.Text.Html)
        }

        post("/login"){
            val name = call.receiveText()
            call.sessions.set(RedSession(name = name))
            call.respond(HttpStatusCode.OK)
        }

        post("/upload") { _ ->

            val session = call.sessions.get<RedSession>()
            val name = session!!.name

            // retrieve all multipart data (suspending)
            val multipart = call.receiveMultipart()
            val path = System.getProperty("user.dir")
            var dir = File("$path/uploads/$name")
            dir.mkdirs()

            multipart.forEachPart { part ->
                // if part is a file (could be form item)
                if (part is PartData.FileItem) {
                    // retrieve file name of upload
                    val name = part.originalFileName!!
                    val file = File(dir, name)

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

            call.respond(HttpStatusCode.OK)
        }
    }
}

fun main() {
    embeddedServer(Netty, 8080, watchPaths = listOf("BlogAppKt"), module = Application::module).start()
}