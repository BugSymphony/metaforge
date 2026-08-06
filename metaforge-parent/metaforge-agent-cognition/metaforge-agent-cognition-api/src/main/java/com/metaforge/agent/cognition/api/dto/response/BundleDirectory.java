package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;

public class BundleDirectory {

    private List<BundleEntry> bundles;

    public List<BundleEntry> getBundles() { return bundles; }
    public void setBundles(List<BundleEntry> bundles) { this.bundles = bundles; }

    public static class BundleEntry {
        private String fqn;
        private String name;
        private String description;
        private String owner;
        private boolean isSystem;
        private List<SubjectDomainGroup> domainTree;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public boolean isSystem() { return isSystem; }
        public void setSystem(boolean isSystem) { this.isSystem = isSystem; }
        public List<SubjectDomainGroup> getDomainTree() { return domainTree; }
        public void setDomainTree(List<SubjectDomainGroup> domainTree) { this.domainTree = domainTree; }
    }

    public static class SubjectDomainGroup {
        private String fqn;
        private String name;
        private List<SubjectDomain> domains;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<SubjectDomain> getDomains() { return domains; }
        public void setDomains(List<SubjectDomain> domains) { this.domains = domains; }
    }

    public static class SubjectDomain {
        private String fqn;
        private String name;
        private List<Task> tasks;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Task> getTasks() { return tasks; }
        public void setTasks(List<Task> tasks) { this.tasks = tasks; }
    }

    public static class Task {
        private String fqn;
        private String name;

        public String getFqn() { return fqn; }
        public void setFqn(String fqn) { this.fqn = fqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
