import java.io.File
import org.apache.commons.vfs2._

object VTest extends App {

  import com.intridea.io.vfs.provider.s3.S3FileProvider
  import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider

  val bucket: String = {
    if (args.length!=1) {
      println("Usage: VTest bucketName")
      System.exit(-2)
    }
    args(0)
  }

  val fsManager: FileSystemManager = {
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

  val s3Utils    = new VFileUtils(new S3FileProvider, fsManager)
  val localUtils = new VFileUtils(new DefaultLocalFileProvider, fsManager)

  val dir: FileObject = try {
    s3Utils.resolveFile("s3://vfs-test")
  } catch {
    case npe: NullPointerException =>
      println("Did you provide AWS credentials?")
      sys.exit(-1)
  }
  dir.createFolder()

  val dest: FileObject = fsManager.resolveFile(s"s3://$bucket/README.md")
  val src: FileObject = fsManager.resolveFile(new File("README.md").getAbsolutePath)
  dest.delete() // copyFrom fails if the destination already exists, so try to delete it first
  dest.copyFrom(src, Selectors.SELECT_SELF)
}
