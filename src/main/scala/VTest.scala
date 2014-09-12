import java.io.File
import org.apache.commons.vfs2._

object VTest extends App {
  val fsManager = {
    import java.io.FileInputStream
    import java.util.Properties
    import com.intridea.io.vfs.provider.s3.S3FileProvider
    import org.apache.commons.vfs2.auth.StaticUserAuthenticator
    import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder

    val config = new Properties()
    config.load(new FileInputStream(System.getProperty("user.home") + "/.aws/config")) // same authentication file used by aws-cli
    val auth = new StaticUserAuthenticator(null, config.getProperty("aws_access_key_id"), config.getProperty("aws_secret_access_key"))
    val opts = S3FileProvider.getDefaultFileSystemOptions
    DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth)
    VFS.getManager
  }

  val dir: FileObject = try {
    fsManager.resolveFile("s3://vfs-test")
  } catch {
    case npe: NullPointerException =>
      println("Did you provide AWS credentials?")
      sys.exit(-1)
  }
  dir.createFolder()

  // Upload file to S3
  val dest: FileObject = fsManager.resolveFile("s3://vfs-test/README.md")
  val src: FileObject = fsManager.resolveFile(new File("README.md").getAbsolutePath)
  dest.copyFrom(src, Selectors.SELECT_SELF)
}
