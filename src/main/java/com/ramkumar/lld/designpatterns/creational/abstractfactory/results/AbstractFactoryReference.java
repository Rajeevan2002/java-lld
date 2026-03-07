package com.ramkumar.lld.designpatterns.creational.abstractfactory.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Reference Solution — Abstract Factory Pattern (Creational)
 * Phase 3, Topic 3.3 | Scenario B: Cloud Infrastructure Provider
 *
 * Key fixes over the practice submission:
 *   1. IllegalStateException (not NoSuchElementException) in all database insert/query
 *   2. upload() validates only data != null — blank strings ARE valid data
 *   3. download() throws with actual bucket+"/"+key value, not a literal string
 *   4. All internal maps are private final
 *   5. No %n\n — just %n in printf statements
 */
public class AbstractFactoryReference {

    // =========================================================================
    // ABSTRACT PRODUCTS — three column interfaces in the family grid
    // =========================================================================

    // [AbstractProduct A] — Compute
    interface ComputeInstance {
        String launch(int cpuCores, int memoryGb);
        void terminate(String instanceId);
        String getProviderName();
        double getCostPerHour(int cpuCores);
    }

    // [AbstractProduct B] — Storage
    interface ObjectStorage {
        void upload(String bucket, String key, String data);
        String download(String bucket, String key);
        String getProviderName();
    }

    // [AbstractProduct C] — Database
    interface ManagedDatabase {
        void createTable(String tableName);
        void insert(String tableName, String record);
        List<String> query(String tableName);
        String getProviderName();
    }

    // =========================================================================
    // ABSTRACT FACTORY — one factory method per product type
    // =========================================================================

    // [AbstractFactory]
    interface CloudProviderFactory {
        ComputeInstance createCompute();
        ObjectStorage   createStorage();
        ManagedDatabase createDatabase();
    }

    // =========================================================================
    // AWS FAMILY — Row 1 of the grid
    // =========================================================================

    // [ConcreteProduct — AWS, Compute]
    static class AWSComputeInstance implements ComputeInstance {
        // Instance field (NOT static) — each AWSComputeInstance has its own counter
        private int counter = 0;

        @Override
        public String launch(int cpuCores, int memoryGb) {
            if (cpuCores < 1) throw new IllegalArgumentException("cpuCores must be >= 1");
            if (memoryGb < 1) throw new IllegalArgumentException("memoryGb must be >= 1");
            String id = String.format("aws-i-%05d", ++counter);
            // [Fix 5] — only %n, not %n\n — exactly one newline
            System.out.printf("[EC2] Launching %d-core/%dGB → %s%n", cpuCores, memoryGb, id);
            return id;
        }

        @Override
        public void terminate(String instanceId) {
            if (instanceId == null || instanceId.isBlank())
                throw new IllegalArgumentException("instanceId must not be blank");
            System.out.println("[EC2] Terminating " + instanceId);
        }

        @Override public String getProviderName() { return "AWS"; }
        @Override public double getCostPerHour(int cpuCores) { return cpuCores * 0.048; }
    }

    // [ConcreteProduct — AWS, Storage]
    static class AWSS3Storage implements ObjectStorage {
        // [Fix 4] — private final; the reference never changes after construction
        private final Map<String, String> store = new HashMap<>();

        @Override
        public void upload(String bucket, String key, String data) {
            if (bucket == null || bucket.isBlank())
                throw new IllegalArgumentException("bucket must not be blank");
            if (key == null || key.isBlank())
                throw new IllegalArgumentException("key must not be blank");
            // [Fix 2] — only null is invalid; blank strings are valid data
            if (data == null) throw new IllegalArgumentException("data must not be null");
            store.put(bucket + "/" + key, data);
            System.out.printf("[S3] Uploaded %s/%s (%d bytes)%n", bucket, key, data.length());
        }

        @Override
        public String download(String bucket, String key) {
            if (bucket == null || bucket.isBlank())
                throw new IllegalArgumentException("bucket must not be blank");
            if (key == null || key.isBlank())
                throw new IllegalArgumentException("key must not be blank");
            String storeKey = bucket + "/" + key;
            if (!store.containsKey(storeKey))
                // [Fix 3] — evaluate the actual path, not a literal description of the variables
                throw new NoSuchElementException(storeKey + " not found");
            return store.get(storeKey);
        }

        @Override public String getProviderName() { return "AWS"; }
    }

    // [ConcreteProduct — AWS, Database]
    static class AWSRDSDatabase implements ManagedDatabase {
        // [Fix 4] — private final
        private final Map<String, List<String>> tables = new HashMap<>();

        @Override
        public void createTable(String tableName) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            tables.put(tableName, new ArrayList<>());
            System.out.println("[RDS] Created table '" + tableName + "'");
        }

        @Override
        public void insert(String tableName, String record) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            if (record == null || record.isBlank())
                throw new IllegalArgumentException("record must not be blank");
            // [Fix 1] — IllegalStateException: the object's state (no such table) makes this illegal
            if (!tables.containsKey(tableName))
                throw new IllegalStateException("Table not found: " + tableName);
            tables.get(tableName).add(record);
            System.out.printf("[RDS] INSERT into '%s': %s%n", tableName, record);
        }

        @Override
        public List<String> query(String tableName) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            // [Fix 1] — IllegalStateException, not NoSuchElementException
            if (!tables.containsKey(tableName))
                throw new IllegalStateException("Table not found: " + tableName);
            return Collections.unmodifiableList(tables.get(tableName));
        }

        @Override public String getProviderName() { return "AWS"; }
    }

    // [ConcreteFactory — AWS]
    static class AWSCloudFactory implements CloudProviderFactory {
        @Override public ComputeInstance createCompute()  { return new AWSComputeInstance(); }
        @Override public ObjectStorage   createStorage()  { return new AWSS3Storage(); }
        @Override public ManagedDatabase createDatabase() { return new AWSRDSDatabase(); }
    }

    // =========================================================================
    // AZURE FAMILY — Row 2 of the grid
    // =========================================================================

    // [ConcreteProduct — Azure, Compute]
    static class AzureVMInstance implements ComputeInstance {
        private int counter = 0;

        @Override
        public String launch(int cpuCores, int memoryGb) {
            if (cpuCores < 1) throw new IllegalArgumentException("cpuCores must be >= 1");
            if (memoryGb < 1) throw new IllegalArgumentException("memoryGb must be >= 1");
            String id = String.format("azure-vm-%05d", ++counter);
            System.out.printf("[AzureVM] Launching %d-core/%dGB → %s%n", cpuCores, memoryGb, id);
            return id;
        }

        @Override
        public void terminate(String instanceId) {
            if (instanceId == null || instanceId.isBlank())
                throw new IllegalArgumentException("instanceId must not be blank");
            System.out.println("[AzureVM] Terminating " + instanceId);
        }

        @Override public String getProviderName() { return "Azure"; }
        @Override public double getCostPerHour(int cpuCores) { return cpuCores * 0.052; }
    }

    // [ConcreteProduct — Azure, Storage]
    static class AzureBlobStorage implements ObjectStorage {
        private final Map<String, String> store = new HashMap<>();

        @Override
        public void upload(String bucket, String key, String data) {
            if (bucket == null || bucket.isBlank())
                throw new IllegalArgumentException("bucket must not be blank");
            if (key == null || key.isBlank())
                throw new IllegalArgumentException("key must not be blank");
            if (data == null) throw new IllegalArgumentException("data must not be null");
            store.put(bucket + "/" + key, data);
            System.out.printf("[Blob] Uploaded %s/%s (%d bytes)%n", bucket, key, data.length());
        }

        @Override
        public String download(String bucket, String key) {
            if (bucket == null || bucket.isBlank())
                throw new IllegalArgumentException("bucket must not be blank");
            if (key == null || key.isBlank())
                throw new IllegalArgumentException("key must not be blank");
            String storeKey = bucket + "/" + key;
            if (!store.containsKey(storeKey))
                throw new NoSuchElementException(storeKey + " not found");
            return store.get(storeKey);
        }

        @Override public String getProviderName() { return "Azure"; }
    }

    // [ConcreteProduct — Azure, Database]
    static class AzureSQLDatabase implements ManagedDatabase {
        private final Map<String, List<String>> tables = new HashMap<>();

        @Override
        public void createTable(String tableName) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            tables.put(tableName, new ArrayList<>());
            System.out.println("[AzureSQL] Created table '" + tableName + "'");
        }

        @Override
        public void insert(String tableName, String record) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            if (record == null || record.isBlank())
                throw new IllegalArgumentException("record must not be blank");
            if (!tables.containsKey(tableName))
                throw new IllegalStateException("Table not found: " + tableName);
            tables.get(tableName).add(record);
            System.out.printf("[AzureSQL] INSERT into '%s': %s%n", tableName, record);
        }

        @Override
        public List<String> query(String tableName) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            if (!tables.containsKey(tableName))
                throw new IllegalStateException("Table not found: " + tableName);
            return Collections.unmodifiableList(tables.get(tableName));
        }

        @Override public String getProviderName() { return "Azure"; }
    }

    // [ConcreteFactory — Azure]
    static class AzureCloudFactory implements CloudProviderFactory {
        @Override public ComputeInstance createCompute()  { return new AzureVMInstance(); }
        @Override public ObjectStorage   createStorage()  { return new AzureBlobStorage(); }
        @Override public ManagedDatabase createDatabase() { return new AzureSQLDatabase(); }
    }

    // =========================================================================
    // GCP FAMILY — Row 3 of the grid
    // =========================================================================

    // [ConcreteProduct — GCP, Compute]
    static class GCPComputeEngine implements ComputeInstance {
        private int counter = 0;

        @Override
        public String launch(int cpuCores, int memoryGb) {
            if (cpuCores < 1) throw new IllegalArgumentException("cpuCores must be >= 1");
            if (memoryGb < 1) throw new IllegalArgumentException("memoryGb must be >= 1");
            String id = String.format("gcp-gce-%05d", ++counter);
            System.out.printf("[GCE] Launching %d-core/%dGB → %s%n", cpuCores, memoryGb, id);
            return id;
        }

        @Override
        public void terminate(String instanceId) {
            if (instanceId == null || instanceId.isBlank())
                throw new IllegalArgumentException("instanceId must not be blank");
            System.out.println("[GCE] Terminating " + instanceId);
        }

        @Override public String getProviderName() { return "GCP"; }
        @Override public double getCostPerHour(int cpuCores) { return cpuCores * 0.044; }
    }

    // [ConcreteProduct — GCP, Storage]
    static class GCPCloudStorage implements ObjectStorage {
        private final Map<String, String> store = new HashMap<>();

        @Override
        public void upload(String bucket, String key, String data) {
            if (bucket == null || bucket.isBlank())
                throw new IllegalArgumentException("bucket must not be blank");
            if (key == null || key.isBlank())
                throw new IllegalArgumentException("key must not be blank");
            if (data == null) throw new IllegalArgumentException("data must not be null");
            store.put(bucket + "/" + key, data);
            System.out.printf("[GCS] Uploaded %s/%s (%d bytes)%n", bucket, key, data.length());
        }

        @Override
        public String download(String bucket, String key) {
            if (bucket == null || bucket.isBlank())
                throw new IllegalArgumentException("bucket must not be blank");
            if (key == null || key.isBlank())
                throw new IllegalArgumentException("key must not be blank");
            String storeKey = bucket + "/" + key;
            if (!store.containsKey(storeKey))
                throw new NoSuchElementException(storeKey + " not found");
            return store.get(storeKey);
        }

        @Override public String getProviderName() { return "GCP"; }
    }

    // [ConcreteProduct — GCP, Database]
    static class GCPBigQuery implements ManagedDatabase {
        private final Map<String, List<String>> tables = new HashMap<>();

        @Override
        public void createTable(String tableName) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            tables.put(tableName, new ArrayList<>());
            System.out.println("[BigQuery] Created table '" + tableName + "'");
        }

        @Override
        public void insert(String tableName, String record) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            if (record == null || record.isBlank())
                throw new IllegalArgumentException("record must not be blank");
            if (!tables.containsKey(tableName))
                throw new IllegalStateException("Table not found: " + tableName);
            tables.get(tableName).add(record);
            System.out.printf("[BigQuery] INSERT into '%s': %s%n", tableName, record);
        }

        @Override
        public List<String> query(String tableName) {
            if (tableName == null || tableName.isBlank())
                throw new IllegalArgumentException("tableName must not be blank");
            if (!tables.containsKey(tableName))
                throw new IllegalStateException("Table not found: " + tableName);
            return Collections.unmodifiableList(tables.get(tableName));
        }

        @Override public String getProviderName() { return "GCP"; }
    }

    // [ConcreteFactory — GCP]
    static class GCPCloudFactory implements CloudProviderFactory {
        @Override public ComputeInstance createCompute()  { return new GCPComputeEngine(); }
        @Override public ObjectStorage   createStorage()  { return new GCPCloudStorage(); }
        @Override public ManagedDatabase createDatabase() { return new GCPBigQuery(); }
    }

    // =========================================================================
    // CLIENT — uses only interfaces; zero concrete class names
    // =========================================================================

    // [Client]
    static class InfrastructureDeployer {
        // [Abstract references — never LightButton/DarkButton, never AWSComputeInstance]
        private final ComputeInstance compute;
        private final ObjectStorage   storage;
        private final ManagedDatabase database;

        // [Abstract Factory injected — the single decision point for the whole family]
        InfrastructureDeployer(CloudProviderFactory factory) {
            this.compute  = factory.createCompute();
            this.storage  = factory.createStorage();
            this.database = factory.createDatabase();
        }

        String deploy(String appName, int cpuCores, int memoryGb) {
            System.out.println("[Deployer] Deploying '" + appName + "' on " + compute.getProviderName());
            // [Polymorphism] — same three calls work for AWS, Azure, GCP without if/switch
            String id = compute.launch(cpuCores, memoryGb);
            database.createTable(appName + "_events");
            database.insert(appName + "_events", "Deployed: " + id);
            storage.upload("app-configs", appName + "/latest", "instance=" + id);
            System.out.printf("[Deployer] Cost: $%.3f/hr%n", compute.getCostPerHour(cpuCores));
            return id;
        }

        String getProvider() { return compute.getProviderName(); }
    }

    // =========================================================================
    // Tests — same 13 as practice + Test 14 (catches ISE vs NSE bug)
    // =========================================================================
    public static void main(String[] args) {

        System.out.println("═══ Test 1: AWS compute returns aws-i- prefix ══════════════");
        ComputeInstance awsCompute = new AWSCloudFactory().createCompute();
        String awsInstanceId = awsCompute.launch(4, 16);
        System.out.println("Instance ID: " + awsInstanceId);
        System.out.println("Test 1 " + (awsInstanceId.startsWith("aws-i-") ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 2: Azure compute returns azure-vm- prefix ═════════");
        ComputeInstance azureCompute = new AzureCloudFactory().createCompute();
        String azureInstanceId = azureCompute.launch(2, 8);
        System.out.println("Instance ID: " + azureInstanceId);
        System.out.println("Test 2 " + (azureInstanceId.startsWith("azure-vm-") ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 3: GCP compute returns gcp-gce- prefix ════════════");
        ComputeInstance gcpCompute = new GCPCloudFactory().createCompute();
        String gcpInstanceId = gcpCompute.launch(8, 32);
        System.out.println("Instance ID: " + gcpInstanceId);
        System.out.println("Test 3 " + (gcpInstanceId.startsWith("gcp-gce-") ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 4: AWS family — all products return 'AWS' ══════════");
        CloudProviderFactory awsFactory = new AWSCloudFactory();
        boolean t4 = "AWS".equals(awsFactory.createCompute().getProviderName())
                  && "AWS".equals(awsFactory.createStorage().getProviderName())
                  && "AWS".equals(awsFactory.createDatabase().getProviderName());
        System.out.println("Test 4 " + (t4 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 5: Azure family — all return 'Azure' ═══════════════");
        CloudProviderFactory azureFactory = new AzureCloudFactory();
        boolean t5 = "Azure".equals(azureFactory.createCompute().getProviderName())
                  && "Azure".equals(azureFactory.createStorage().getProviderName())
                  && "Azure".equals(azureFactory.createDatabase().getProviderName());
        System.out.println("Test 5 " + (t5 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 6: GCP family — all return 'GCP' ═══════════════════");
        CloudProviderFactory gcpFactory = new GCPCloudFactory();
        boolean t6 = "GCP".equals(gcpFactory.createCompute().getProviderName())
                  && "GCP".equals(gcpFactory.createStorage().getProviderName())
                  && "GCP".equals(gcpFactory.createDatabase().getProviderName());
        System.out.println("Test 6 " + (t6 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 7: ObjectStorage — upload then download ════════════");
        ObjectStorage s3 = new AWSCloudFactory().createStorage();
        s3.upload("my-bucket", "config/app.json", "{\"env\":\"prod\"}");
        String downloaded = s3.download("my-bucket", "config/app.json");
        System.out.println("Downloaded: " + downloaded);
        System.out.println("Test 7 " + ("{\"env\":\"prod\"}".equals(downloaded) ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 8: ManagedDatabase — createTable + insert + query ══");
        ManagedDatabase rds = new AWSCloudFactory().createDatabase();
        rds.createTable("users");
        rds.insert("users", "alice@example.com");
        rds.insert("users", "bob@example.com");
        List<String> users = rds.query("users");
        System.out.println("Records: " + users);
        boolean t8 = users.size() == 2
                  && users.contains("alice@example.com")
                  && users.contains("bob@example.com");
        System.out.println("Test 8 " + (t8 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 9: ManagedDatabase.query returns unmodifiable list ═");
        try {
            users.add("hacker@evil.com");
            System.out.println("Test 9 FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 9 PASSED — query() returns unmodifiable list");
        }

        System.out.println("\n═══ Test 10: InfrastructureDeployer with AWS ════════════════");
        InfrastructureDeployer awsDeployer = new InfrastructureDeployer(new AWSCloudFactory());
        String awsDeploy = awsDeployer.deploy("payment-service", 2, 8);
        boolean t10 = awsDeploy.startsWith("aws-i-") && "AWS".equals(awsDeployer.getProvider());
        System.out.println("Test 10 " + (t10 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 11: InfrastructureDeployer with Azure ══════════════");
        InfrastructureDeployer azureDeployer = new InfrastructureDeployer(new AzureCloudFactory());
        String azureDeploy = azureDeployer.deploy("payment-service", 2, 8);
        boolean t11 = azureDeploy.startsWith("azure-vm-") && "Azure".equals(azureDeployer.getProvider());
        System.out.println("Test 11 " + (t11 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 12: InfrastructureDeployer with GCP ════════════════");
        InfrastructureDeployer gcpDeployer = new InfrastructureDeployer(new GCPCloudFactory());
        String gcpDeploy = gcpDeployer.deploy("payment-service", 2, 8);
        boolean t12 = gcpDeploy.startsWith("gcp-gce-") && "GCP".equals(gcpDeployer.getProvider());
        System.out.println("Test 12 " + (t12 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 13: Same deployer code, three providers ════════════");
        CloudProviderFactory[] factories = {
            new AWSCloudFactory(), new AzureCloudFactory(), new GCPCloudFactory()
        };
        String[] expectedPrefixes  = { "aws-i-", "azure-vm-", "gcp-gce-" };
        String[] expectedProviders = { "AWS", "Azure", "GCP" };
        boolean t13 = true;
        for (int i = 0; i < factories.length; i++) {
            InfrastructureDeployer d = new InfrastructureDeployer(factories[i]);
            String id = d.deploy("analytics", 4, 16);
            boolean ok = id.startsWith(expectedPrefixes[i])
                      && expectedProviders[i].equals(d.getProvider());
            System.out.println("  " + expectedProviders[i] + ": id=" + id + " → " + (ok ? "OK" : "FAIL"));
            if (!ok) t13 = false;
        }
        System.out.println("Test 13 " + (t13 ? "PASSED" : "FAILED"));

        // ── Test 14: insert() into missing table must throw IllegalStateException ──
        // Most common mistake: using NoSuchElementException (for iterators) instead of
        // IllegalStateException (for invalid object state).
        // Tests 1-13 never trigger this path — they always createTable() before insert().
        System.out.println("\n═══ Test 14: insert() into missing table throws ISE (not NSE) ═");
        ManagedDatabase freshDb = new AWSCloudFactory().createDatabase();
        try {
            freshDb.insert("nonexistent_table", "some record");
            System.out.println("Test 14 FAILED — should have thrown");
        } catch (IllegalStateException e) {
            System.out.println("Caught ISE: " + e.getMessage());
            System.out.println("Test 14 PASSED — correct exception type");
        } catch (NoSuchElementException e) {
            System.out.println("Caught NSE (wrong type): " + e.getMessage());
            System.out.println("Test 14 FAILED — should be ISE, not NSE");
        }
    }
}
