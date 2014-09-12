import java.io.File
import org.apache.commons.vfs2._

object VTest extends App {
  val fsManager: FileSystemManager = VFS.getManager // fails here
  val dir: FileObject = fsManager.resolveFile("s3://vfs-test")
  dir.createFolder()

  // Upload file to S3
  val dest: FileObject = fsManager.resolveFile("s3://vfs-test/README.md")
  val src: FileObject = fsManager.resolveFile(new File("README.md").getAbsolutePath)
  dest.copyFrom(src, Selectors.SELECT_SELF)
}
