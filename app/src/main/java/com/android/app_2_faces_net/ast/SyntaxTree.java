package com.android.app_2_faces_net.ast;

public class SyntaxTree {
    public AbstractNode root;

    public SyntaxTree() {
        this.root = new RootNode();
    }

    public AbstractNode getRoot() {
        return root;
    }


    @Override
    public String toString() {
        return root.toString();
    }
}
