## Overview
The docx document is from resume's attachment, saved by java to local. That can't be open by unzip tool of java or POI or soffice.

## Solution
Using commons-compress.jar to unzip and zip that docx.

## To be continued
Don't know the reason.

## Code
* zip
```
    public static void zipDirByCommonsCompress(File dir, String savePath) throws Exception {
        ZipArchiveOutputStream stream =  new ZipArchiveOutputStream(new File(savePath));
        zipByCommonsCompress(dir, "", stream);
        stream.finish();
        stream.close();
    }

    private static void zipByCommonsCompress(File file, String parentPath, ZipArchiveOutputStream stream) throws Exception {
        if (file.isDirectory()) {
            parentPath += file.getName() + File.separator;
             File[] files = file.listFiles();
             for (File f : files) {
            	 zipByCommonsCompress(f, parentPath, stream);
             }
        } else {
               InputStream in = new FileInputStream(file);
               int i = parentPath.indexOf("\\");
               parentPath = i == -1 ? parentPath : parentPath.substring(i + 1);
               ZipArchiveEntry entry = new ZipArchiveEntry(file, parentPath + file.getName());
               stream.putArchiveEntry(entry);
               IOUtils.copy(in, stream);
               stream.closeArchiveEntry();
               in.close();
        }
    }
```
* unzip
```
 public static boolean unZipByCommonsCompress(File zipFile, String destDir) {
        if(StringUtils.isBlank(destDir)) {
            destDir = zipFile.getParent();
        }

        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        ZipArchiveInputStream is = null;

        try {
            is = new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zipFile), 1024));
            ZipArchiveEntry entry = null;

            while ((entry = is.getNextZipEntry()) != null) {

                if (entry.isDirectory()) {
                    File directory = new File(destDir, entry.getName());
                    directory.mkdirs();
                } else {
                    if (entry.getName().lastIndexOf("/") != -1) {
                        String tempDir = entry.getName().substring(0, entry.getName().lastIndexOf("/"));
                        tempDir = tempDir.endsWith(File.separator) ? tempDir : tempDir + File.separator;
                        File directory = new File(destDir, tempDir);
                        directory.mkdirs();
                    }
                    OutputStream os = null;
                    try {
                        os = new BufferedOutputStream(new FileOutputStream(new File(destDir, entry.getName())), 1024);
                        IOUtils.copy(is, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                    }
                }
            }
            IOUtils.closeQuietly(is);
            return true;
        } catch(Exception e) {
            IOUtils.closeQuietly(is);
            return false;
        }
    }
```