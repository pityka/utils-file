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

  def createFileInTempFolderIfPossibleWithName(fileName: String): File = {
    val f = new File(folder, fileName)
    val success = f.createNewFile
    if (success) f
    else createTempFile(suffix = fileName)
  }

  val writtenExecutables = collection.mutable.Map[String, File]()

  def getExecutableFromJar(name: String): File =
    writtenExecutables.get(name).getOrElse {
      synchronized {
        val f = writeFreshExecutable(name)

        writtenExecutables.update(name, f)

        f
      }
    }

  private def writeFreshExecutable(name: String): File = {
    val tmpFile = createTempFile(".executable")
    tmpFile.deleteOnExit()
    tmpFile.setExecutable(true)

    val d = readStreamAndClose(getClass().getResource(name).openStream()).toArray
    writeBinaryToFile(tmpFile.getAbsolutePath, d)
    tmpFile

  }

}
