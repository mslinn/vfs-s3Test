# Virtual File System Test #

See [abashev/vfs-s3](https://github.com/abashev/vfs-s3), the [jets3t samples](https://jets3t.s3.amazonaws.com/toolkit/code-samples.html) and the [http://commons.apache.org/proper/commons-vfs/apidocs/index.html](commons-vfs Javadoc)

## Setup ##
You might want to install [AWS CLI](http://aws.amazon.com/cli/)

Here is a typical configuration file for AWS CLI. This demo uses the same configuration file:

````
$ cat ~/.aws/config
[default]
aws_access_key_id = AKIADFIUYFEWFOEWFJLEWHF
aws_secret_access_key = JI1OKdRTrXMudidsdfoiwpeofiwepoifpew
region = us-east-1
````

## Running ##
This demo requires an AWS S3 bucket that your credentials have read/write access to.
Let's call that bucket `mybucket`. You can run the demo like this:

    $ sbt "runMain VTest mybucket"
    $ aws s3 ls mybucket
