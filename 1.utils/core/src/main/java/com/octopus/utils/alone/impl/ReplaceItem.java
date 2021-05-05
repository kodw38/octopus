package com.octopus.utils.alone.impl;

import java.util.Map;

public class ReplaceItem {
        String startMark;
        String endMark;
        String replaceContent;
        Map<String,String> replaceFileName;
        String[] deleteLineStartWith;

    public Map<String, String> getReplaceFileName() {
        return replaceFileName;
    }

    public void setReplaceFileName(Map<String, String> replaceFileName) {
        this.replaceFileName = replaceFileName;
    }

    public String[] getDeleteLineStartWith() {
        return deleteLineStartWith;
    }

    public void setDeleteLineStartWith(String[] deleteLineStartWith) {
        this.deleteLineStartWith = deleteLineStartWith;
    }

    public String getExcludeForReplaceInLine() {
        return excludeForReplaceInLine;
    }

    public void setExcludeForReplaceInLine(String excludeForReplaceInLine) {
        this.excludeForReplaceInLine = excludeForReplaceInLine;
    }

    public Map<String, String> getReplaceInLine() {
        return replaceInLine;
    }

    public void setReplaceInLine(Map<String, String> replaceInLine) {
        this.replaceInLine = replaceInLine;
    }

    String excludeForReplaceInLine;
        Map<String,String> replaceInLine;

        public String getStartMark() {
            return startMark;
        }

        public void setStartMark(String startMark) {
            this.startMark = startMark;
        }

        public String getEndMark() {
            return endMark;
        }

        public void setEndMark(String endMark) {
            this.endMark = endMark;
        }

        public String getReplaceContent() {
            return replaceContent;
        }

        public void setReplaceContent(String replaceContent) {
            this.replaceContent = replaceContent;
        }



}
