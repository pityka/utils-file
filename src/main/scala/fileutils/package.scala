import scala.io.Source
import java.io.{
  PrintWriter,
  BufferedWriter,
  FileWriter,
  FileInputStream,
  FileOutputStream,
  BufferedOutputStream,
  LineNumberReader,
  InputStream,
  BufferedReader,
  FileReader,
  BufferedInputStream
}
import java.util.zip.GZIPInputStream

import java.io.EOFException
import java.io.File
import scala.sys.process._
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.charset.CodingErrorAction
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.io.Codec

package object fileutils {

  import TempFile._

  /** Searches $PATH for the specified file */
  def searchForFileInPath(fileName: String) =
    System
      .getenv("PATH")
      .split(java.util.regex.Pattern.quote(File.pathSeparator))
      .exists(f => new File(f, fileName).canRead)

  /**
    * Creates symbolic links with the same basename but different extensions.
    *
    * @param filesWithExtensions files with the needed extensions
    * @return A temporary File which points to the common part of the symbolic links paths.
    */
  def putBesides(filesWithExtensions: (File, String)*): File = {

    val tmp = createTempFile("common")

    filesWithExtensions.foreach {
      case (file, ext) =>
        val filepath = java.nio.file.Paths.get(file.getCanonicalPath)
        val filelinkpath = java.nio.file.Paths.get(tmp.getCanonicalPath + ext)
        java.nio.file.Files.createSymbolicLink(filelinkpath, filepath)
    }

    tmp

  }

  def createSymbolicLink(from: File, to: File): File = {
    val fromP = java.nio.file.Paths.get(from.getCanonicalPath)
    val toP = java.nio.file.Paths.get(to.getCanonicalPath)
    java.nio.file.Files.createSymbolicLink(fromP, toP)
    from
  }

  /**
    * Returns the result of the block, and closes the resource.
    *
    * @param param closeable resource
    * @param f block using the resource
    */
  def useResource[A <: { def close(): Unit }, B](param: A)(f: A => B): B =
    try { f(param) } finally {
      param.close()
    }

  /** Alias for [[useResource]] */
  def using[A <: { def close(): Unit }, B] = useResource[A, B] _

  def readLines(s: String): Vector[String] = openSource(s)(_.getLines.toVector)
  def readLines(s: File): Vector[String] = readLines(s.getAbsolutePath)

  def openFileInputStreamMaybeZipped[T](file: File)(func: InputStream => T) =
    if (isGZipFile(file.getAbsolutePath)) openZippedFileInputStream(file)(func)
    else openFileInputStream(file)(func)

  /**
    * Opens a [[scala.io.Source]], executes the block, then closes the source.
    *
    * @param s Path of the file
    */
  def openSource[A](s: String)(f: io.Source => A)(implicit codec: Codec): A =
    if (isGZipFile(s)) openZippedSource(s)(f)(codec)
    else useResource(scala.io.Source.fromFile(s)(codec))(f)

  def createSource(s: File)(implicit codec: Codec) =
    if (isGZipFile(s)) createSourceFromZippedFile(s.getAbsolutePath)(codec)
    else scala.io.Source.fromFile(s)(codec)

  /**
    * Opens a [[scala.io.Source]], executes the block, then closes the source.
    *
    * @param s Path of the file
    */
  def openSource[A](s: File)(f: io.Source => A)(implicit codec: Codec): A =
    openSource(s.getAbsolutePath)(f)(codec)

  /** Returns a [[scala.io.Source]] from the given GZipped file. */
  def createSourceFromZippedFile(file: String,
                                 bufferSize: Int = io.Source.DefaultBufSize)(
      implicit codec: io.Codec): io.BufferedSource = {
    val inputStream = new BufferedInputStream(
      new GZIPInputStream(new FileInputStream(file), 65536 * 32),
      65536 * 32)

    io.Source.createBufferedSource(
      inputStream,
      bufferSize,
      () => createSourceFromZippedFile(file, bufferSize)(codec),
      () => inputStream.close()
    )(codec) withDescription ("file:" + new java.io.File(file).getAbsolutePath)
  }

  /** Returns true if the file is GZipped. */
  def isGZipFile(file: String): Boolean =
    scala.util
      .Try(
        new java.util.zip.GZIPInputStream(
          new java.io.FileInputStream(new java.io.File(file))))
      .isSuccess

  /** Returns true if the file is GZipped. */
  def isGZipFile(file: File): Boolean = isGZipFile(file.getAbsolutePath)

  private[this] def openZippedSource[A](s: String)(f: io.Source => A)(
      codec: Codec) =
    useResource(createSourceFromZippedFile(s)(codec))(f)

  /** Writes text data to file. */
  def writeToFile(fileName: String, data: java.lang.String): Unit =
    useResource(new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
      writer =>
        writer.write(data)
    }

  /** Writes text data to file. */
  def writeToFile(file: File, data: String): Unit =
    writeToFile(file.getAbsolutePath, data)

  /** Writes binary data to file. */
  def writeBinaryToFile(fileName: String, data: Array[Byte]): Unit =
    useResource(new BufferedOutputStream(new FileOutputStream(fileName))) {
      writer =>
        writer.write(data)
    }

  /** Writes binary data to file. */
  def writeBinaryToFile(file: File, data: Array[Byte]): Unit =
    writeBinaryToFile(file.getAbsolutePath, data)

  def writeToTempFile(data: String): File = {
    val tmp = createTempFile("")
    writeToFile(tmp, data)
    tmp
  }

  def writeBinaryToTempFile(data: Array[Byte]): File = {
    val tmp = createTempFile("")
    writeBinaryToFile(tmp, data)
    tmp
  }

  /** Writes the ByteBuffer's contents to a file. */
  def writeByteBufferToFile(fileName: String, buff: java.nio.ByteBuffer) =
    useResource(new FileOutputStream(fileName)) { writer =>
      val channel = writer.getChannel
      buff.rewind
      channel.write(buff)
      channel.close
    }

  /** Writes the ByteBuffer's contents to a file. */
  def writeByteBufferToFile(file: File, buff: java.nio.ByteBuffer): Unit =
    writeByteBufferToFile(file.getAbsolutePath, buff)

  /**
    * Appends text to file.
    * Opens a new [[java.io.FileWriter]] on every call.
    */
  def appendToFile(fileName: String, textData: String): Unit =
    useResource(new BufferedWriter(new FileWriter(fileName, true))) { writer =>
      writer.write(textData)
    }

  /**
    * Appends text to file.
    * Opens a new [[java.io.FileWriter]] on every call.
    */
  def appendToFile(file: File, data: String): Unit =
    appendToFile(file.getAbsolutePath, data)

  /** Returns true if file is empty. */
  def fileIsEmpty(file: File): Boolean = {
    var b: Int = 0
    openFileReader(file) { r =>
      b = r.read
    }
    b == -1
  }

  /** Opens a [[java.io.BufferedWriter]] on the file. Closes it after the block is executed. */
  def openFileWriter[T](fileName: File, append: Boolean = false)(
      func: BufferedWriter => T): T =
    useResource(new BufferedWriter(new FileWriter(fileName, append)))(func)

  def openFileWriter[T](func: BufferedWriter => T): (File, T) = {
    val t = TempFile.createTempFile("tmp")
    val x = openFileWriter(t)(func)
    (t, x)
  }

  /** Opens an unbuffered [[java.io.Writer]] on the file. Closes it after the block is executed. */
  def openUnbufferedFileWriter[T](fileName: File, append: Boolean = false)(
      func: java.io.Writer => T) =
    useResource(new FileWriter(fileName, append))(func)

  /** Opens a buffered [[java.io.BufferedOutputStream]] on the file. Closes it after the block is executed. */
  def openFileOutputStream[T](fileName: File, append: Boolean = false)(
      func: BufferedOutputStream => T): T =
    useResource(
      new BufferedOutputStream(new FileOutputStream(fileName, append)))(func)

  /** Opens a buffered [[java.io.BufferedOutputStream]] on the file. Closes it after the block is executed. */
  def openFileOutputStream[T](func: BufferedOutputStream => T): (File, T) = {
    val t = TempFile.createTempFile("tmp")
    val x = openFileOutputStream(t)(func)
    (t, x)
  }

  /** Opens a buffered [[java.io.BufferedInputStream]] on the file. Closes it after the block is executed. */
  def openFileInputStream[T](fileName: File)(func: BufferedInputStream => T) =
    useResource(new BufferedInputStream(new FileInputStream(fileName)))(func)

  /** Opens a buffered [[java.io.BufferedReader]] on the file. Closes it after the block is executed. */
  def openFileReader[T](fileName: File)(func: BufferedReader => T): T = {
    if (isGZipFile(fileName.getAbsolutePath))
      openZippedFileReader(fileName)(func)
    else useResource(new BufferedReader(new FileReader(fileName)))(func)
  }

  /** Opens a [[java.io.BufferedReader]] on a GZipped file. Closes it after the block is executed. */
  def openZippedFileReader[T](fileName: File)(func: BufferedReader => T): T = {
    useResource(
      new BufferedReader(
        new java.io.InputStreamReader(new BufferedInputStream(
          new GZIPInputStream(new FileInputStream(fileName), 65536),
          65536))))(func)
  }

  /** Opens a [[java.io.BufferedWriter]] which writes GZip compressed data to the given path. Closes it after the block is executed. */
  def openZippedFileWriter[T](fileName: File, append: Boolean = false)(
      func: java.io.Writer => T): T =
    useResource(
      new BufferedWriter(
        new java.io.OutputStreamWriter(new java.util.zip.GZIPOutputStream(
          new FileOutputStream(fileName, append)))))(func)

  def openZippedFileWriter[T](func: java.io.Writer => T): (File, T) = {
    val t = TempFile.createTempFile("tmp")
    val x = openZippedFileWriter(t)(func)
    (t, x)
  }

  /** Opens a [[java.io.OutputStream]] which writes GZip compressed data to the given path. Closes it after the block is executed. */
  def openZippedFileOutputStream[T](fileName: File, append: Boolean = false)(
      func: java.io.OutputStream => T): T =
    useResource(
      new java.util.zip.GZIPOutputStream(
        new FileOutputStream(fileName, append)))(func)

  def openZippedFileOutputStream[T](
      func: java.io.OutputStream => T): (File, T) = {
    val t = TempFile.createTempFile("tmp")
    val x = openZippedFileOutputStream(t)(func)
    (t, x)
  }

  /** Opens a [[java.io.OutputStream]] which writes GZip compressed data to the given path. Closes it after the block is executed. */
  def openZippedFileInputStream[T](fileName: File)(
      func: java.io.InputStream => T) =
    useResource(
      new java.util.zip.GZIPInputStream(new FileInputStream(fileName)))(func)

  /** Returns a [[java.io.BufferedReader]]. */
  def getFileReader(fileName: File) =
    new BufferedReader(new FileReader(fileName))

  /** Returns a [[java.io.BufferedWriter]]. */
  def getFileWriter(fileName: File, append: Boolean = false) =
    new BufferedWriter(new FileWriter(fileName, append))

  /** Reads file contents into a bytearray. */
  def readBinaryFile(fileName: String): Array[Byte] = {
    useResource(new BufferedInputStream(new FileInputStream(fileName))) { f =>
      readBinaryStream(f)
    }
  }

  /**
    * Returns an iterator on the InputStream's data.
    *
    * Closes the stream when read through.
    */
  def readStreamAndClose(is: java.io.InputStream) = new Iterator[Byte] {
    var s = is.read

    def hasNext = s != -1

    def next = {
      var x = s.toByte; s = is.read; if (!hasNext) { is.close() }; x
    }
  }

  /** Reads file contents into a bytearray. */
  def readBinaryFile(f: File): Array[Byte] = readBinaryFile(f.getAbsolutePath)

  /** Reads file contents into a bytearray. */
  def readBinaryStream(f: java.io.InputStream): Array[Byte] = {
    def read(x: scala.collection.mutable.ArrayBuffer[Byte])
      : scala.collection.mutable.ArrayBuffer[Byte] = {
      val raw = f.read
      val ch: Byte = raw.toByte
      if (raw != -1) {
        x.append(ch)
        read(x)
      } else {
        x
      }
    }
    read(scala.collection.mutable.ArrayBuffer[Byte]()).toArray
  }

  /** Concatenates text files together, dropping header lines for each, except the first. */
  def cat(in: Iterable[File], out: File, header: Int = 0) {
    var skip = 0
    openFileWriter(out, false) { writer =>
      in.foreach { f =>
        openSource(f.getAbsolutePath) {
          _.getLines.drop(skip).foreach { line =>
            writer.write(line)
            writer.write('\n')
          }
        }
        skip = header
      }
    }
  }

  implicit def stringToProcess(command: String): ProcessBuilder =
    Process(command)
  implicit def stringSeqToProcess(command: Seq[String]): ProcessBuilder =
    Process(command)

  /**
    * Execute command with user function to process each line of output.
    *
    * Based on from http://www.jroller.com/thebugslayer/entry/executing_external_system_commands_in
    * Creates 3 new threads: one for the stdout, one for the stderror, and one waits for the exit code.
    * @param pb Description of the executable process
    * @param atMost Maximum time to wait for the process to complete. Default infinite.
    * @return Exit code of the process.
    */
  def exec(pb: ProcessBuilder,
           atMost: Duration = Duration.Inf,
           retries: Int = 0)(stdOutFunc: String => Unit = { x: String =>
    })(implicit stdErrFunc: String => Unit = (x: String) => ()): Int = {

    import scala.util._
    def retry[A](i: Int)(f: => A): A = Try(f) match {
      case Success(x)                               => x
      case Failure(e: java.io.IOException) if i > 0 => retry(i - 1)(f)
      case Failure(e)                               => throw e
    }

    import java.util.concurrent.Executors

    val executorService = Executors.newSingleThreadExecutor

    implicit val ec = ExecutionContext.fromExecutorService(executorService)

    val process = retry(retries)(pb.run(ProcessLogger(stdOutFunc, stdErrFunc)))

    val hook = try {
      scala.sys.addShutdownHook { process.destroy() }
    } catch {
      case x: Throwable => {
        Try(process.destroy)
        Try(executorService.shutdownNow)
        throw x
      }
    }

    try {
      val f = Future { process.exitValue }
      Await.result(f, atMost = atMost)
    } finally {
      Try(process.destroy)
      Try(executorService.shutdownNow)
      Try(hook.remove)

    }
  }

  /**
    * Execute command. Returns stdout and stderr as strings, and true if it was successful.
    *
    * A process is considered successful if its exit code is 0 and the error stream is empty.
    * The latter criterion can be disabled with the unsuccessfulOnErrorStream parameter.
    * @param pb The process description.
    * @param unsuccessfulOnErrorStream if true, then the process is considered as a failure if its stderr is not empty.
    * @param atMost max waiting time.
    * @return (stdout,stderr,success) triples
    */
  def execGetStreamsAndCode(
      pb: ProcessBuilder,
      unsuccessfulOnErrorStream: Boolean = true,
      atMost: Duration = Duration.Inf,
      retries: Int = 0): (List[String], List[String], Boolean) = {
    var ls: List[String] = Nil
    var lse: List[String] = Nil
    var boolean = true
    val exitvalue = exec(pb, atMost, retries) { ln =>
      ls = ln :: ls
    } { ln =>
      if (unsuccessfulOnErrorStream) { boolean = false }; lse = ln :: lse
    }
    (ls.reverse, lse.reverse, boolean && (exitvalue == 0))
  }

  /**
    * Execute command. Returns stdout and stderr as strings, and true if it was successful. Also writes to log.
    *
    * A process is considered successful if its exit code is 0 and the error stream is empty.
    * The latter criterion can be disabled with the unsuccessfulOnErrorStream parameter.
    * @param pb The process description.
    * @param unsuccessfulOnErrorStream if true, then the process is considered as a failure if its stderr is not empty.
    * @param atMost max waiting time.
    * @param log A logger.
    * @return (stdout,stderr,success) triples
    */
  def execGetStreamsAndCodeWithLog(pb: ProcessBuilder,
                                   unsuccessfulOnErrorStream: Boolean = true,
                                   atMost: Duration = Duration.Inf,
                                   retries: Int = 0)(implicit log: {
    def info(s: String): Unit; def error(s: String): Unit
  }): (List[String], List[String], Boolean) = {
    var ls: List[String] = Nil
    var lse: List[String] = Nil
    var boolean = true
    val exitvalue = exec(pb, atMost, retries) { ln =>
      ls = ln :: ls; log.info(ln)
    } { ln =>
      if (unsuccessfulOnErrorStream) { boolean = false }; lse = ln :: lse;
      if (unsuccessfulOnErrorStream) log.error(ln) else log.info(ln)
    }
    (ls.reverse, lse.reverse, boolean && (exitvalue == 0))
  }

}
