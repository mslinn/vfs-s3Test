import org.apache.commons.vfs2.provider.AbstractFileProvider
import org.apache.commons.vfs2.{FileSystemManager, FileObject}


/** VFS equivalents of Apache Commons FileUtils required for GitBucket.
  * As much as possible, this is a literal translation from Java to Scala. */
object VFileUtils {
  import java.io.IOException
  import java.nio.charset.Charset

  import org.apache.commons.io.{IOUtils, Charsets}

  implicit class RichFileObject(fileObject: FileObject) {
    import org.apache.commons.vfs2.FileType
    import java.io.File

    def getName: String = fileObject.getName.getBaseName

    def isDirectory: Boolean = fileObject.getType==FileType.FOLDER

    def isFile: Boolean = fileObject.getType==FileType.FILE

    def listFiles: Array[FileObject] = if (isDirectory) fileObject.getChildren else Array.empty

    def toFile: File = new File(fileObject.getName.getPath)

    def toAbsoluteName: String = fileObject.getName.getPathDecoded

    def toAbsoluteFile: File = fileObject.toFile.getAbsoluteFile

    /** Only works for LocalFile providers */
    def toCanonicalFile: File = {
      val canonicalDir: File = fileObject.toFile.getParentFile.getCanonicalFile
      new File(canonicalDir + fileObject.getName.getBaseName).getCanonicalFile
    }

    /** Only works for LocalFile providers */
    def toCanonicalName: String = toCanonicalFile.getCanonicalPath
  }

  /** Cleans a directory without deleting it.
   * @param directory directory to clean
   * @throws IOException in case cleaning is unsuccessful */
  def cleanDirectory(directory: FileObject): Unit = {
      if (!directory.exists) {
          val message = directory + " does not exist"
          throw new IllegalArgumentException(message)
      }

      if (!directory.isDirectory) {
          val message = directory + " is not a directory"
          throw new IllegalArgumentException(message)
      }

      val files = directory.listFiles
      if (files == null) {
        // null if security restricted
          throw new IOException("Failed to list contents of " + directory)
      }

      var exception: IOException = null;
      files foreach { file =>
          try {
              forceDelete(file)
          } catch {
            case ioe: IOException =>
              exception = ioe
          }
      }

      if (null != exception) {
          throw exception
      }
  }

  /**
   * Copies a whole directory to a new location preserving the file dates.
   * <p>
   * This method copies the specified directory and all its child
   * directories and files to the specified destination.
   * The destination is the new location and name of the directory.
   * <p>
   * The destination directory is created if it does not exist.
   * If the destination directory did exist, then this method merges
   * the source with the destination, with the source taking precedence.
   * <p>
   * <strong>Note:</strong> This method tries to preserve the files' last
   * modified date/times using {@link File#setLastModified(long)}, however
   * it is not guaranteed that those operations will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * @param srcDir  an existing directory to copy, must not be {@code null}
   * @param destDir  the new directory, must not be {@code null}
   *
   * @throws NullPointerException if source or destination is {@code null}
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since 1.1
   */
  def copyDirectory(srcDir: FileObject, destDir: FileObject): Unit =
    copyDirectory(srcDir, destDir, preserveFileDate = true)

  /**
   * Copies a whole directory to a new location.
   * <p>
   * This method copies the contents of the specified source directory
   * to within the specified destination directory.
   * <p>
   * The destination directory is created if it does not exist.
   * If the destination directory did exist, then this method merges
   * the source with the destination, with the source taking precedence.
   * <p>
   * <strong>Note:</strong> Setting <code>preserveFileDate</code> to
   * {@code true} tries to preserve the files' last modified
   * date/times using {@link File#setLastModified(long)}, however it is
   * not guaranteed that those operations will succeed.
   * If the modification operation fails, no indication is provided.
   *
   * @param srcDir  an existing directory to copy, must not be {@code null}
   * @param destDir  the new directory, must not be {@code null}
   * @param preserveFileDate  true if the file date of the copy
   *  should be the same as the original
   *
   * @throws NullPointerException if source or destination is {@code null}
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @since 1.1
   */
  def copyDirectory(srcDir: FileObject, destDir:FileObject, preserveFileDate: Boolean): Unit = {
    import org.apache.commons.vfs2.FileSelector
    import org.apache.commons.vfs2.FileSelectInfo

    val fileSelector = new FileSelector() {
      override def includeFile(fileInfo: FileSelectInfo): Boolean = true

      override def traverseDescendents(fileInfo: FileSelectInfo): Boolean = true
    }
    srcDir.copyFrom(destDir, fileSelector)
  }

  /** Deletes a directory recursively.
   * @param directory  directory to delete
   * @throws IOException in case deletion is unsuccessful */
  def deleteDirectory(directory: FileObject): Unit = {
      if (!directory.exists)
          return

      if (!isSymlink(directory))
          cleanDirectory(directory)

      if (!directory.delete()) {
          val message = "Unable to delete directory " + directory + "."
          throw new IOException(message)
      }
  }

  def deleteQuietly(fileObject: FileObject): Boolean = {
    if (fileObject == null)
      return false
    try {
      if (fileObject.isDirectory) {
        cleanDirectory(fileObject)
      }
    } catch {
      case ignored:Exception =>
    }

    try {
      fileObject.delete()
    } catch {
      case ignored: Exception =>
        false
    }
  }

  def forceDelete(file: FileObject): Unit = {
    if (file.isDirectory) {
      deleteDirectory(file)
    } else {
      val filePresent = file.exists
      if (!file.delete()) {
        if (!filePresent) {
          throw new java.io.FileNotFoundException("File does not exist: " + file)
        }
        val message = "Unable to delete file: " + file
        throw new IOException(message)
      }
    }
  }


  /**
   * Determines whether the specified file is a Symbolic Link on a local file system rather than an actual file.
   * <p>
   * Will not return true if there is a Symbolic Link anywhere in the path,
   * only if the specific file is.
   * <p>
   * <b>Note:</b> the current implementation always returns {@code false} if the system
   * is detected as Windows using {@link FilenameUtils#isSystemWindows()}
   *
   * @param fileObject the file to check
   * @return true if the file is a Symbolic Link
   * @throws IOException if an IO error occurs while checking the file
   * @since 2.0
   */
  def isSymlink(fileObject: FileObject): Boolean = {
    import java.io.File
    import org.apache.commons.vfs2.provider.local.LocalFile
    import org.apache.commons.io.FileUtils

    if (fileObject == null)
      throw new NullPointerException("File must not be null")

    fileObject match {
      case localFile: LocalFile =>
        false

      case file =>
        try {
          val rootFile = fileObject.getFileSystem.getRootName.getRoot
          val fileName = rootFile + fileObject.getName.getPathDecoded
          val file = new File(fileName)
          FileUtils.isSymlink(file)
        } catch {
          case e: Exception =>
            throw new RuntimeException("Error checking if symlink", e)
        }
    }
  }

  /**
   * Reads the contents of a file into a String. The file is always closed.
   *
   * @param fileObject
   *            the file to read, must not be {@code null}
   * @param encoding
   *            the encoding to use, {@code null} means platform default
   * @return the file contents, never {@code null}
   * @throws IOException
   *             in case of an I/O error
   * @throws UnsupportedCharsetException
   *             thrown instead of {@link UnsupportedEncodingException} in version 2.2 if the encoding is not
   *             supported.
   * @since 2.3
   */
  def readFileToString(fileObject: FileObject, encoding: String): String =
    readFileToString(fileObject, Charsets.toCharset(encoding))

  /**
   * Reads the contents of a file into a String.
   * The file is always closed.
   *
   * @param fileObject  the file to read, must not be {@code null}
   * @param encoding  the encoding to use, {@code null} means platform default
   * @return the file contents, never {@code null}
   * @throws IOException in case of an I/O error
   * @since 2.3
   */
  def readFileToString(fileObject: FileObject, encoding: Charset): String =
    IOUtils.toString(fileObject.getContent.getInputStream, encoding)

  def writeByteArrayToFile(fileObject: FileObject, data: Array[Byte]): Unit =
    writeByteArrayToFile(fileObject, data, append = false)

  def writeByteArrayToFile(fileObject: FileObject, data: Array[Byte], append: Boolean): Unit = {
    import java.io.OutputStream
    import org.apache.commons.io.IOUtils

    var out: OutputStream = null
    try {
      out = fileObject.getContent.getOutputStream(append)
      out.write(data)
      out.close() // don't swallow close Exception if copy completes normally
    } finally {
      IOUtils.closeQuietly(out)
    }
  }
}

class VFileUtils(provider: AbstractFileProvider, fsManager: FileSystemManager) {
  import com.intridea.io.vfs.provider.s3.S3FileProvider
  import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider
  import org.apache.commons.vfs2.FileObject

  // TODO delete this
  val resolveFile: String => FileObject = provider match {
    case s3Provider: S3FileProvider =>
      fsManager.resolveFile(_: String)

    case localProvider: DefaultLocalFileProvider =>
      fsManager.resolveFile(_: String)
  }
}
