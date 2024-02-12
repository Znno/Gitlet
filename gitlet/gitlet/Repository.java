package gitlet;

import java.io.File;
import java.util.*;
import java.util.List;

import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author Elabyad & Znno
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    public static final File STAGING_DIR = join(GITLET_DIR, "staging");
    public static final File ADD_STAGE_FILE = join(STAGING_DIR, "add");
    public static final File REMOVE_STAGE_FILE = join(STAGING_DIR, "remove");
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");
    public static final File BRANCH_DIR = join(GITLET_DIR, "branches");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File BRANCHES = join(GITLET_DIR, "branchesMap");
    public static final File CURRENT_BRANCH = join(GITLET_DIR, "current_branch");

    public static void init() {
        if (GITLET_DIR.exists()) {
            String msg = "A Gitlet version-control system already exists in the current directory.";
            System.out.println(msg);
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        COMMITS_DIR.mkdir();
        STAGING_DIR.mkdir();
        writeObject(ADD_STAGE_FILE, new TreeMap<String, String>());
        writeObject(REMOVE_STAGE_FILE, new TreeMap<String, String>());
        BLOBS_DIR.mkdir();
        BRANCH_DIR.mkdir();
        Branch master = new Branch("master", null);
        Commit intialCommit = new Commit();
        master.setBranchHead(intialCommit.getUID());
        intialCommit.saveCommit();
        writeContents(HEAD, intialCommit.getUID());
        master.saveBranch();
        writeContents(CURRENT_BRANCH, master.getUID());
        TreeMap<String, String> brMap = new TreeMap<>();
        brMap.put("master", master.getUID());
        writeObject(BRANCHES, brMap);
    }

    public static void add(String name) {
        Blob newBlob = new Blob(name);
        String sha = readContentsAsString(HEAD);
        File file = join(COMMITS_DIR, sha);
        Commit currentCommit = readObject(file, Commit.class);
        TreeMap<String, String> addMap = readObject(ADD_STAGE_FILE, TreeMap.class);
        if (currentCommit.getBlob(name) == null) {
            addMap.put(name, newBlob.getUID());
            newBlob.saveBlob();
        } else {
            if (currentCommit.getBlob(name).equals(newBlob.getUID())) {
                addMap.remove(name);
            } else {
                addMap.put(name, newBlob.getUID());
                newBlob.saveBlob();
            }
        }
        writeObject(ADD_STAGE_FILE, addMap);
        TreeMap<String, String> removeMap = readObject(REMOVE_STAGE_FILE, TreeMap.class);
        if (removeMap.get(name) != null) {
            removeMap.remove(name);
        }
        writeObject(REMOVE_STAGE_FILE, removeMap);

    }

    public static void rm(String name) {
        TreeMap<String, String> addMap = readObject(ADD_STAGE_FILE, TreeMap.class);
        String sha = readContentsAsString(HEAD);
        File file = join(COMMITS_DIR, sha);
        Commit currentCommit = readObject(file, Commit.class);
        if (addMap.get(name) == null && currentCommit.getBlob(name) == null) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (addMap.get(name) != null) {
            addMap.remove(name);
            writeObject(ADD_STAGE_FILE, addMap);
        }
        if (currentCommit.getBlob(name) != null) {
            TreeMap<String, String> removeMap = readObject(REMOVE_STAGE_FILE, TreeMap.class);
            removeMap.put(name, currentCommit.getBlob(name));
            File temp = join(CWD, name);
            temp.delete();
            writeObject(REMOVE_STAGE_FILE, removeMap);
        }
    }

    public static void commit(String message, String secondParent) {
        TreeMap<String, String> addMap = readObject(ADD_STAGE_FILE, TreeMap.class);
        TreeMap<String, String> removemap = readObject(REMOVE_STAGE_FILE, TreeMap.class);
        if (addMap.isEmpty() && removemap.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        String sha = readContentsAsString(HEAD);
        File file = join(COMMITS_DIR, sha);
        Commit currentCommit = readObject(file, Commit.class);
        Commit newCommit = new Commit(message, currentCommit.getUID(), secondParent);
        newCommit.addBlobs(currentCommit.getBlobs());
        newCommit.addBlobs(addMap);
        newCommit.removeBlobs(removemap);
        addMap.clear();
        removemap.clear();
        writeObject(ADD_STAGE_FILE, addMap);
        writeObject(REMOVE_STAGE_FILE, removemap);
        newCommit.saveCommit();
        writeContents(HEAD, newCommit.getUID());
        String currBranch = readContentsAsString(CURRENT_BRANCH);
        File fileBranch = join(BRANCH_DIR, currBranch);
        Branch curB = readObject(fileBranch, Branch.class);
        curB.setBranchHead(newCommit.getUID());
        curB.saveBranch();
        TreeMap<String, String> brMap = readObject(BRANCHES, TreeMap.class);
        brMap.put(curB.getName(), curB.getUID());
        writeObject(BRANCHES, brMap);
    }

    public static void log() {
        String sha = readContentsAsString(HEAD);
        File file = join(COMMITS_DIR, sha);
        Commit currentCommit = readObject(file, Commit.class);
        while (currentCommit != null) {
            System.out.println("===");
            System.out.println("commit " + currentCommit.getUID());
            Date time = currentCommit.getDate();
            Formatter f = new Formatter().format("%1$ta %1$tb %1$td %1$tT %1$tY %1$tz", time);
            String fDate = f.toString();
            System.out.println("Date: " + fDate);
            System.out.println(currentCommit.getMessage());
            System.out.println();
            if (currentCommit.getParent() == null) {
                break;
            }
            File nextCommitFile = join(COMMITS_DIR, currentCommit.getParent());
            currentCommit = readObject(nextCommitFile, Commit.class);
        }
    }

    public static void globalLog() {
        List<String> files = plainFilenamesIn(COMMITS_DIR);
        for (String commitID : files) {
            File file = join(COMMITS_DIR, commitID);
            Commit currentCommit = readObject(file, Commit.class);
            System.out.println("===");
            System.out.println("commit " + currentCommit.getUID());
            Date time = currentCommit.getDate();
            Formatter f = new Formatter().format("%1$ta %1$tb %1$td %1$tT %1$tY %1$tz", time);
            String fDate = f.toString();
            System.out.println("Date: " + fDate);
            System.out.println(currentCommit.getMessage());
            System.out.println();
        }
    }

    public static void find(String message) {
        List<String> files = plainFilenamesIn(COMMITS_DIR);
        boolean found = false;
        for (String commitID : files) {
            File file = join(COMMITS_DIR, commitID);
            Commit currentCommit = readObject(file, Commit.class);
            if (!(currentCommit.getMessage().equals(message))) {
                continue;
            }
            System.out.println(currentCommit.getUID());
            found = true;
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }

    }

    public static void checkoutFile(String fileName) {
        String sha = readContentsAsString(HEAD);
        File file = join(COMMITS_DIR, sha);
        Commit currentCommit = readObject(file, Commit.class);
        String blobID = currentCommit.getBlob(fileName);
        if (blobID == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File blobFile = join(BLOBS_DIR, blobID);
        Blob fileBlob = readObject(blobFile, Blob.class);

        File cwdFile = join(CWD, fileName);
        writeContents(cwdFile, fileBlob.getContent());

    }

    public static void checkoutCommit(String commitID, String fileName) {
        File commitFile = join(COMMITS_DIR, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit checkCommit = readObject(commitFile, Commit.class);
        String blobID = checkCommit.getBlob(fileName);
        if (blobID == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File blobFile = join(BLOBS_DIR, blobID);
        Blob fileBlob = readObject(blobFile, Blob.class);
        File cwdFile = join(CWD, fileName);
        writeContents(cwdFile, fileBlob.getContent());
    }

    public static void status() {
        System.out.println("=== Branches ===");
        TreeMap<String, String> brMap = readObject(BRANCHES, TreeMap.class);
        String currBranch = readContentsAsString(CURRENT_BRANCH);
        File file = join(BRANCH_DIR, currBranch);
        Branch curB = readObject(file, Branch.class);
        for (Map.Entry<String, String> set : brMap.entrySet()) {
            if (set.getKey().equals(curB.getName())) {
                System.out.print("*");
            }
            System.out.println(set.getKey());
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        TreeMap<String, String> addMap = readObject(ADD_STAGE_FILE, TreeMap.class);
        for (Map.Entry<String, String> set : addMap.entrySet()) {
            System.out.println(set.getKey());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        TreeMap<String, String> removeMap = readObject(REMOVE_STAGE_FILE, TreeMap.class);
        for (Map.Entry<String, String> set : removeMap.entrySet()) {
            System.out.println(set.getKey());
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void branch(String branchName) {
        TreeMap<String, String> brMap = readObject(BRANCHES, TreeMap.class);
        if (brMap.get(branchName) != null) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String currHead = readContentsAsString(HEAD);
        Branch newBranch = new Branch(branchName, currHead);
        newBranch.saveBranch();
        brMap.put(branchName, newBranch.getUID());
        writeObject(BRANCHES, brMap);
    }

    public static void checkoutBranch(String branchName) {
        TreeMap<String, String> brMap = readObject(BRANCHES, TreeMap.class);
        if (brMap.get(branchName) == null) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String currBranch = readContentsAsString(CURRENT_BRANCH);
        File bFile = join(BRANCH_DIR, currBranch);
        Branch curB = readObject(bFile, Branch.class);
        if (branchName.equals(curB.getName())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        String branchSha = brMap.get(branchName);
        File branchFile = join(BRANCH_DIR, branchSha);
        Branch newBranch = readObject(branchFile, Branch.class);
        String branchHead = newBranch.getBranchHead();
        File newCommitFile = join(COMMITS_DIR, branchHead);
        Commit newCommit = readObject(newCommitFile, Commit.class);

        String commitSha = readContentsAsString(HEAD);
        File currentCommitFile = join(COMMITS_DIR, commitSha);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);

        TreeMap<String, String> addMap = readObject(ADD_STAGE_FILE, TreeMap.class);
        TreeMap<String, String> removeMap = readObject(REMOVE_STAGE_FILE, TreeMap.class);
        List<String> files = plainFilenamesIn(CWD);
        boolean valid = true;
        TreeMap<String, String> untracked = new TreeMap<String, String>();
        for (String fileName : files) {
            if (removeMap.get(fileName) != null) {
                untracked.put(fileName, fileName);
            }
            if (addMap.get(fileName) == null && currentCommit.getBlobs().get(fileName) == null) {
                untracked.put(fileName, fileName);
            }
        }
        for (Map.Entry<String, String> set : untracked.entrySet()) {
            if (newCommit.getBlobs().get(set.getValue()) != null) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            String m1 = "There is an untracked file in the way;";
            String m2 = " delete it, or add and commit it first.";
            System.out.println(m1 + m2);
            System.exit(0);
        }
        for (Map.Entry<String, String> set : newCommit.getBlobs().entrySet()) {
            File file = join(BLOBS_DIR, set.getValue());
            Blob ourBlob = readObject(file, Blob.class);
            String content = ourBlob.getContent();
            File overwrite = join(CWD, set.getKey());
            writeContents(overwrite, content);
        }
        List<String> newfiles = plainFilenamesIn(CWD);
        for (String fileName : newfiles) {
            if (newCommit.getBlobs().get(fileName) == null) {
                File deletedfile = join(CWD, fileName);
                deletedfile.delete();
            }
        }
        addMap.clear();
        removeMap.clear();
        writeContents(CURRENT_BRANCH, newBranch.getUID());
        writeObject(ADD_STAGE_FILE, addMap);
        writeObject(REMOVE_STAGE_FILE, removeMap);
        writeContents(HEAD, newCommit.getUID());
        brMap.put(newBranch.getName(), newBranch.getUID());
        writeObject(BRANCHES, brMap);
    }

    public static void rmbranch(String branchName) {
        TreeMap<String, String> brMap = readObject(BRANCHES, TreeMap.class);
        if (brMap.get(branchName) == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String currBranch = readContentsAsString(CURRENT_BRANCH);
        File bFile = join(BRANCH_DIR, currBranch);
        Branch bBranch = readObject(bFile, Branch.class);
        if (bBranch.getName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        String branchSha = brMap.get(branchName);
        File branchFile = join(BRANCH_DIR, branchSha);
        branchFile.delete();
        brMap.remove(branchName);
        writeObject(BRANCHES, brMap);
    }

    public static void reset(String commitID) {
        File commitFile = join(COMMITS_DIR, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        String currBranch = readContentsAsString(CURRENT_BRANCH);
        File file = join(BRANCH_DIR, currBranch);
        Branch curB = readObject(file, Branch.class);

        Commit newCommit = readObject(commitFile, Commit.class);

        String commitSha = readContentsAsString(HEAD);
        File currentCommitFile = join(COMMITS_DIR, commitSha);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);

        TreeMap<String, String> addMap = readObject(ADD_STAGE_FILE, TreeMap.class);
        TreeMap<String, String> removeMap = readObject(REMOVE_STAGE_FILE, TreeMap.class);

        List<String> files = plainFilenamesIn(CWD);
        boolean valid = true;
        TreeMap<String, String> untracked = new TreeMap<String, String>();
        for (String fileName : files) {
            if (removeMap.get(fileName) != null) {
                untracked.put(fileName, fileName);
            }
            if (addMap.get(fileName) == null && currentCommit.getBlobs().get(fileName) == null) {
                untracked.put(fileName, fileName);
            }
        }
        for (Map.Entry<String, String> set : untracked.entrySet()) {
            if (newCommit.getBlobs().get(set.getValue()) != null) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            String m1 = "There is an untracked file in the way;";
            String m2 = " delete it, or add and commit it first.";
            System.out.println(m1 + m2);
            System.exit(0);
        }

        for (Map.Entry<String, String> set : newCommit.getBlobs().entrySet()) {
            File blobFile = join(BLOBS_DIR, set.getValue());
            Blob ourBlob = readObject(blobFile, Blob.class);
            String content = ourBlob.getContent();
            File overwrite = join(CWD, set.getKey());
            writeContents(overwrite, content);
        }
        List<String> newfiles = plainFilenamesIn(CWD);
        for (String fileName : newfiles) {
            if (newCommit.getBlob(fileName) == null) {
                File deletedfile = join(CWD, fileName);
                deletedfile.delete();
            }
        }
        writeContents(HEAD, newCommit.getUID());
        curB.setBranchHead(newCommit.getUID());
        curB.saveBranch();
        TreeMap<String, String> brMap = readObject(BRANCHES, TreeMap.class);
        brMap.put(curB.getName(), curB.getUID());
        addMap.clear();
        removeMap.clear();
        writeObject(ADD_STAGE_FILE, addMap);
        writeObject(REMOVE_STAGE_FILE, removeMap);
        writeObject(BRANCHES, brMap);
        writeContents(CURRENT_BRANCH, curB.getUID());
    }

    public static void merge(String branchName) {
        TreeMap<String, String> brMap = readObject(BRANCHES, TreeMap.class);
        if (brMap.get(branchName) == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        TreeMap<String, String> addMap = readObject(ADD_STAGE_FILE, TreeMap.class);
        TreeMap<String, String> removeMap = readObject(REMOVE_STAGE_FILE, TreeMap.class);
        if (!addMap.isEmpty() || !removeMap.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        String currBranch = readContentsAsString(CURRENT_BRANCH);
        File file = join(BRANCH_DIR, currBranch);
        Branch curB = readObject(file, Branch.class);
        String currentName = curB.getName();
        if (curB.getName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        String branchSha = brMap.get(branchName);
        File branchFile = join(BRANCH_DIR, branchSha);
        Branch newBranch = readObject(branchFile, Branch.class);
        String branchHead = newBranch.getBranchHead();
        File newCommitFile = join(COMMITS_DIR, branchHead);
        Commit newCommit = readObject(newCommitFile, Commit.class);

        String commitSha = readContentsAsString(HEAD);
        File currentCommitFile = join(COMMITS_DIR, commitSha);
        Commit currentCommit = readObject(currentCommitFile, Commit.class);

        List<String> files = plainFilenamesIn(CWD);
        boolean valid = true;
        TreeMap<String, String> untracked = new TreeMap<String, String>();
        for (String fileName : files) {
            if (removeMap.get(fileName) != null) {
                untracked.put(fileName, fileName);
            }
            if (addMap.get(fileName) == null && currentCommit.getBlobs().get(fileName) == null) {
                untracked.put(fileName, fileName);
            }
        }
        for (Map.Entry<String, String> set : untracked.entrySet()) {
            if (newCommit.getBlobs().get(set.getValue()) != null) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            String m1 = "There is an untracked file in the way;";
            String m2 = " delete it, or add and commit it first.";
            System.out.println(m1 + m2);
            System.exit(0);
        }
        TreeMap<String, Boolean> exist = new TreeMap<String, Boolean>();
        Queue<String> llist = new ArrayDeque<>();
        llist.add(currentCommit.getUID());
        while (!llist.isEmpty()) {
            String node = llist.remove();
            exist.put(node, true);
            File tempCommitFile = join(COMMITS_DIR, node);
            Commit tempCommit = readObject(tempCommitFile, Commit.class);
            if (tempCommit.getParent() != null) {
                llist.add(tempCommit.getParent());
            }
            if (tempCommit.getSecParent() != null) {
                llist.add(tempCommit.getSecParent());
            }
        }
        llist.add(newCommit.getUID());
        String splitPoint = null;
        while (!llist.isEmpty()) {
            String node = llist.remove();
            if (exist.get(node) != null) {
                splitPoint = node;
                break;
            }
            File tempCommitFile = join(COMMITS_DIR, node);
            Commit tempCommit = readObject(tempCommitFile, Commit.class);
            if (tempCommit.getParent() != null) {
                llist.add(tempCommit.getParent());
            }
            if (tempCommit.getSecParent() != null) {
                llist.add(tempCommit.getSecParent());
            }
        }
        if (splitPoint.equals(newCommit.getUID())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint.equals(currentCommit.getUID())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        File splitPointFile = join(COMMITS_DIR, splitPoint);
        Commit splitPointCommit = readObject(splitPointFile, Commit.class);
        TreeMap<String, Boolean> all = new TreeMap<>();
        for (Map.Entry<String, String> set : newCommit.getBlobs().entrySet()) {
            all.put(set.getKey(), true);
        }
        for (Map.Entry<String, String> set : currentCommit.getBlobs().entrySet()) {
            all.put(set.getKey(), true);
        }
        for (Map.Entry<String, String> set : splitPointCommit.getBlobs().entrySet()) {
            all.put(set.getKey(), true);
        }
        boolean conflict = false;
        for (Map.Entry<String, Boolean> set : all.entrySet()) {
            String spBlob = splitPointCommit.getBlobs().get(set.getKey());
            String curBlob = currentCommit.getBlobs().get(set.getKey());
            String newBlob = newCommit.getBlobs().get(set.getKey());
            boolean first = Objects.equals(curBlob, newBlob);
            boolean second = Objects.equals(spBlob, curBlob);
            boolean third = Objects.equals(spBlob, newBlob);
            if (!first && !second && !third) {
                String s = "<<<<<<< HEAD\n";
                if (curBlob != null) {
                    File wantedFile = join(BLOBS_DIR, curBlob);
                    Blob blobObject = readObject(wantedFile, Blob.class);
                    s += blobObject.getContent();
                }
                s += "=======\n";
                if (newBlob != null) {
                    File wantedFile = join(BLOBS_DIR, newBlob);
                    Blob blobObject = readObject(wantedFile, Blob.class);
                    s += blobObject.getContent();
                }
                s += ">>>>>>>\n";
                File blobFule = join(CWD, set.getKey());
                writeContents(blobFule, s);
                add(set.getKey());
                conflict = true;
            }
            if (spBlob != null) {
                if (newBlob != null) {
                    if (spBlob.equals(curBlob) && !spBlob.equals(newBlob)) {
                        File writeOver = join(CWD, set.getKey());
                        File blobFile = join(BLOBS_DIR, newBlob);
                        Blob blobObject = readObject(blobFile, Blob.class);
                        writeContents(writeOver, blobObject.getContent());
                        add(set.getKey());
                    }
                } else {
                    if (spBlob.equals(curBlob)) {
                        rm(set.getKey());
                    }
                }
            } else {
                if (curBlob == null && newBlob != null) {
                    File writeOver = join(CWD, set.getKey());
                    File blobFile = join(BLOBS_DIR, newBlob);
                    Blob blobObject = readObject(blobFile, Blob.class);
                    writeContents(writeOver, blobObject.getContent());
                    add(set.getKey());
                }
            }
        }
        String msg = "Merged " + branchName + " into " + currentName + ".";
        commit(msg, newCommit.getUID());
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }
}
