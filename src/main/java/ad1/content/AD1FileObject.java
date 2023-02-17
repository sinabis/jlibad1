package ad1.content;

import ad1.AD1MetaReader;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AD1FileObject {
    private final int fileClass;
    private final long folderIndex;
    private final String name;
    private final long firstFencePost;
    private final int fencePostCount;
    private final AD1MetaReader ad1MetaReader;

    private String md5sum;
    private String sha1sum;
    private long fileSize;

    @SuppressWarnings("unused")
    private String dateAccessed;

    @SuppressWarnings("unused")
    private String dateCreated;

    @SuppressWarnings("unused")
    private String dateModified;

    @SuppressWarnings("unused")
    private boolean encrypted;

    @SuppressWarnings("unused")
    private boolean compressed;

    @SuppressWarnings("unused")
    private boolean actualFile;

    @SuppressWarnings("unused")
    private boolean hidden;

    @SuppressWarnings("unused")
    private boolean system;

    @SuppressWarnings("unused")
    private boolean readOnly;

    @SuppressWarnings("unused")
    private boolean archive;

    public AD1FileObject(int fileClass, long folderIndex, String name, Map<Integer, Map<Integer, String>> metaData,
                         long firstFencePost, int fencePostCount, AD1MetaReader ad1MetaReader) {
        this.fileClass = fileClass;
        this.folderIndex = folderIndex;
        this.name = name;
        this.firstFencePost = firstFencePost;
        this.fencePostCount = fencePostCount;
        this.ad1MetaReader = ad1MetaReader;


        this.md5sum = "";
        this.sha1sum = "";
        this.fileSize = 0L;
        parseMeta(metaData);
    }

    public String genFullPath() {
        List<String> folderList = new LinkedList<>();
        recurseFolderIndexes(this.folderIndex, folderList);

        folderList.add(this.name);

        return Paths.get(this.ad1MetaReader.getLogicalImagePath(), folderList.toArray(String[]::new)).toString();
    }

    private void recurseFolderIndexes(long folderIndex, List<String> folderList) {
        long parentIndex = Optional.ofNullable(this.ad1MetaReader.getFolderIndexCache().get(folderIndex)).orElse(-1L);
        String folderName = Optional.ofNullable(this.ad1MetaReader.getFolderCache().get(folderIndex)).orElse("");
        if (parentIndex != -1) {
            folderList.add(0, folderName);
            recurseFolderIndexes(parentIndex, folderList);
        } else  {
            folderList.add(0, "");
        }
    }

    public String getName() {
        return name;
    }

    public long getFolderIndex() {
        return folderIndex;
    }

    public long getFirstFencePost() {
        return firstFencePost;
    }

    public int getFencePostCount() {
        return fencePostCount;
    }

    @SuppressWarnings("unused")
    public String getMD5() {
        return md5sum;
    }

    @SuppressWarnings("unused")
    public String getSHA1() {
        return sha1sum;
    }

    public long getFileSize() {
        return fileSize;
    }

    private void parseMeta(Map<Integer, Map<Integer, String>> metaData) {
        if (metaData.get(1) != null) {
            this.md5sum = metaData.get(1).get(20481);
            this.sha1sum = metaData.get(1).get(20482);
        }

        if (metaData.get(3) != null) {
            try {
                this.fileSize = Long.parseLong(metaData.get(3).get(3));
            } catch (NumberFormatException e) {
                this.fileSize = 0;
            }

            if (this.fencePostCount == 0) {
                fileSize = 0;
            }
        }

        if (metaData.get(4) != null) {
            this.encrypted = Boolean.parseBoolean(metaData.get(4).get(13));
            this.compressed = Boolean.parseBoolean(metaData.get(4).get(14));
            this.actualFile = Boolean.parseBoolean(metaData.get(4).get(15));
            this.hidden = Boolean.parseBoolean(metaData.get(4).get(4098));
            this.system = Boolean.parseBoolean(metaData.get(4).get(4099));
            this.readOnly = Boolean.parseBoolean(metaData.get(4).get(4100));
            this.archive = Boolean.parseBoolean(metaData.get(4).get(4101));
        }

        if (metaData.get(5) != null) {
            this.dateAccessed = metaData.get(5).get(7);
            this.dateCreated = metaData.get(5).get(8);
            this.dateModified = metaData.get(5).get(9);
        }
    }

    @SuppressWarnings("unused")
    public int getFileClass() {
        return fileClass;
    }

    @SuppressWarnings("unused")
    public String getDateAccessed() {
        return dateAccessed;
    }

    @SuppressWarnings("unused")
    public String getDateCreated() {
        return dateCreated;
    }

    @SuppressWarnings("unused")
    public String getDateModified() {
        return dateModified;
    }

    @SuppressWarnings("unused")
    public boolean isEncrypted() {
        return encrypted;
    }

    @SuppressWarnings("unused")
    public boolean isCompressed() {
        return compressed;
    }

    @SuppressWarnings("unused")
    public boolean isActualFile() {
        return actualFile;
    }

    @SuppressWarnings("unused")
    public boolean isHidden() {
        return hidden;
    }

    @SuppressWarnings("unused")
    public boolean isSystem() {
        return system;
    }

    @SuppressWarnings("unused")
    public boolean isReadOnly() {
        return readOnly;
    }

    @SuppressWarnings("unused")
    public boolean isArchive() {
        return archive;
    }

    @Override
    public String toString() {
        return "AD1FileObject{" +
                "fileClass=" + fileClass +
                ", folderIndex=" + folderIndex +
                ", name='" + name + '\'' +
                ", firstFencePost=" + firstFencePost +
                ", fencePostCount=" + fencePostCount +
                '}';
    }

     /*
     private static final Map<Integer, o> U = new LinkedHashMap(250);
    public static final o a = b(3, "File Size");
    public static final o b = b(4, "Physical Size");
    public static final o c = a(5, "Compressed File Size");
    public static final o d = b(6, "File Start Cluster");
    public static final o e = b(13, "Encrypted");
    public static final o f = b(14, "Compressed");
    public static final o g = b(30, "Is Actual File");
    public static final o h = b(31, "Start Sector");
    public static final o i = b(36, "Alternate Data Stream Count");
    public static final o j = b(4097, "DOS Short Name");
    public static final o k = a(4098, "MS-DOS Hidden");
    public static final o l = a(4099, "MS-DOS System");
    public static final o m = a(4100, "MS-DOS Read-only");
    public static final o n = a(4101, "MS-DOS Archive");
    public static final o o = a(20481, "Original MD5 Digest");
    public static final o p = a(20482, "Original SHA-1 Digest");
    public static final o q = a(40961, "NTFS MFT Index");
    public static final o r = b(40962, "NTFS MFT Record Date");
    public static final o s = a(40963, "NTFS MFT Resident");
    public static final o t = b(40964, "NTFS Offline");
    public static final o u = b(40965, "NTFS Sparse");
    public static final o v = b(40966, "NTFS Temporary");
    public static final o w = a(40967, "File Owner");
    public static final o x = a(40968, "File Owner");
    public static final o y = a(40969, "File Group");
    public static final o z = a(40970, "File Group");
    public static final o A = c(16777217, "NTFS ACE Type");
    public static final o B = c(16777218, "NTFS ACE Audit Success");
    public static final o C = c(16777219, "NTFS ACE Audit Failure");
    public static final o D = c(16777220, "NTFS Inheritable");
    public static final o E = c(16777221, "NTFS ACE SID");
    public static final o F = c(16777222, "NTFS ACE Name");
    public static final o G = c(16777223, "NTFS ACE Access Mask");
    public static final o H = c(16777224, "NTFS ACE Execute File");
    public static final o I = c(16777225, "NTFS ACE Read Data");
    public static final o J = c(16777226, "NTFS ACE Write Data");
    public static final o K = c(16777227, "NTFS ACE Append Data");
    public static final o L = c(16777228, "NTFS ACE Traverse Folder");
    public static final o M = c(16777229, "NTFS ACE List Folder");
    public static final o N = c(16777230, "NTFS ACE Create Files");
    public static final o O = c(16777231, "NTFS ACE Create Folders");
    public static final o P = c(16777232, "NTFS ACE Delete Sub-Folders and Files");
    public static final o Q = c(16777233, "NTFS ACE Delete");
    public static final o R = c(16777234, "NTFS ACE Read Permissions");
    public static final o S = c(16777235, "NTFS ACE Change Permissions");
    public static final o T = c(16777236, "NTFS ACE Take Ownership");
     */
}