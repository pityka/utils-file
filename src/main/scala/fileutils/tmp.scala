package fileutils

import java.io.File

object TempFile {

  private def createTempDir(baseName: String): File = {
    val baseDir = new File(System.getProperty("java.io.tmpdir"));

    val TEMP_DIR_ATTEMPTS = 5

    var counter = 0
    var b = false
    var t: Option[File] = None
    while (!b && counter < TEMP_DIR_ATTEMPTS) {
      val tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        b = true
        t = Some(tempDir)
      }
      counter += 1
    }

    if (t.isDefined) {
      val x = t.get
      x.deleteOnExit
      x
    } else {
      throw new IllegalStateException("Failed to create directory within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseDir.getAbsolutePath + "/" + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');

    }

  }

  val id: String = {
    val f = new java.text.SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    f.format(new java.util.Date)
  }

  val prefix = "fileutil" + id

  lazy val folder =
    synchronized {
      createTempDir(prefix)
    }

  def createTempFile(suffix: String): File =
    File.createTempFile(prefix, suffix, folder)

  def createTempFolder(suffix: String): File = {
    val file = File.createTempFile(prefix, suffix, folder)
    file.delete
    file.mkdir
    file
  }

  def createFileInTempFolderIfPossibleWithName(fileName: String): File = {
    val f = new File(folder, fileName)
    val success = f.createNewFile
    if (success) f
    else createTempFile(suffix = fileName)
  }

  val writtenExecutables = scala.collection.concurrent.TrieMap[String, File]()

  def getExecutableFromJar(name: String): File =
    writtenExecutables.getOrElseUpdate(name, writeFreshExecutable(name, None))

  def getExecutableFromJar(resourceName: String, fileName: String): File =
    writtenExecutables.getOrElseUpdate(
      resourceName,
      writeFreshExecutable(resourceName, Some(fileName)))

  private def writeFreshExecutable(resourceName: String,
                                   fileName: Option[String]): File =
    synchronized {
      val tmpFile = fileName match {
        case None => createTempFile(".executable")
        case Some(fileName) =>
          new File(createTempFolder("bin"), fileName)
      }

      val inputStream = new java.io.BufferedInputStream(
        getClass().getResource(resourceName).openStream())
      try {
        val buffer = Array.ofDim[Byte](8096)
        openFileOutputStream(tmpFile) { os =>
          var c = inputStream.read(buffer, 0, buffer.length)
          while (c >= 0) {
            os.write(buffer, 0, c)
            c = inputStream.read(buffer, 0, buffer.length)
          }
        }
      } finally { inputStream.close }

      tmpFile.deleteOnExit()
      tmpFile.setExecutable(true)

      tmpFile

    }

}
