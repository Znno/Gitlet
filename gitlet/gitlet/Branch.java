package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;

public class Branch implements Serializable {
    private String name;
    private String branchHead;

    public Branch(String name, String branchHead) {
        this.name = name;
        this.branchHead = branchHead;
    }

    public void setBranchHead(String newHead) {
        branchHead = newHead;
    }

    @Override
    public String toString() {
        String objContent = name + branchHead;
        return objContent;
    }

    public String getUID() {
        return sha1(this.toString());
    }

    public String getName() {
        return name;
    }

    public void saveBranch() {
        File bracnhFile = join(Repository.BRANCH_DIR, getUID());
        writeObject(bracnhFile, this);
    }

    public String getBranchHead() {
        return branchHead;
    }
}
