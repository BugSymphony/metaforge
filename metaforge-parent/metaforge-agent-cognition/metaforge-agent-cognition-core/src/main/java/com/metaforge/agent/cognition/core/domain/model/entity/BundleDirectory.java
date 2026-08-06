package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;

public class BundleDirectory {
    private List<BundleEntry> bundles;
    public static class BundleEntry {
        private String fqn; private String name; private String description; private String owner;
        private boolean isSystem; private List<SubjectDomainGroup> domainTree;
        public String getFqn() { return fqn; } public void setFqn(String f) { this.fqn = f; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
        public String getOwner() { return owner; } public void setOwner(String o) { this.owner = o; }
        public boolean isSystem() { return isSystem; } public void setSystem(boolean s) { this.isSystem = s; }
        public List<SubjectDomainGroup> getDomainTree() { return domainTree; } public void setDomainTree(List<SubjectDomainGroup> d) { this.domainTree = d; }
    }
    public static class SubjectDomainGroup {
        private String fqn; private String name; private List<SubjectDomain> domains;
        public String getFqn() { return fqn; } public void setFqn(String f) { this.fqn = f; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public List<SubjectDomain> getDomains() { return domains; } public void setDomains(List<SubjectDomain> d) { this.domains = d; }
    }
    public static class SubjectDomain {
        private String fqn; private String name; private List<Task> tasks;
        public String getFqn() { return fqn; } public void setFqn(String f) { this.fqn = f; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public List<Task> getTasks() { return tasks; } public void setTasks(List<Task> t) { this.tasks = t; }
    }
    public static class Task {
        private String fqn; private String name;
        public String getFqn() { return fqn; } public void setFqn(String f) { this.fqn = f; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
    }
    public List<BundleEntry> getBundles() { return bundles; } public void setBundles(List<BundleEntry> b) { this.bundles = b; }
}
