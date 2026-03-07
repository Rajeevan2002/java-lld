package com.ramkumar.lld.designpatterns.creational.abstractfactory.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Practice Exercise — Abstract Factory Pattern (Creational)
 * Phase 3, Topic 3.3 | Scenario B: Cloud Infrastructure Provider
 *
 * ═══════════════════════════════════════════════════════════════════════
 * PROBLEM STATEMENT
 * ═══════════════════════════════════════════════════════════════════════
 *
 * You are building an infrastructure automation tool that must work with
 * three cloud providers: AWS, Azure, and GCP. Each provider has its own
 * compute, storage, and database services — but they must NEVER be mixed
 * (you cannot use AWS compute with Azure storage in the same deployment).
 *
 * Implement using the Abstract Factory pattern so that:
 *   1. The InfrastructureDeployer client uses ONLY interfaces — never
 *      concrete AWS/Azure/GCP classes by name.
 *   2. Switching cloud providers is a single line change (swap the factory).
 *   3. Adding a new cloud provider (e.g., DigitalOcean) requires only
 *      new classes — no changes to InfrastructureDeployer.
 *
 * ── ABSTRACT PRODUCT A: ComputeInstance ─────────────────────────────────
 *
 *   String launch(int cpuCores, int memoryGb)
 *     - Validates cpuCores >= 1 → IllegalArgumentException("cpuCores must be >= 1")
 *     - Validates memoryGb >= 1 → IllegalArgumentException("memoryGb must be >= 1")
 *     - Increments a per-instance (NOT static) counter
 *     - Returns a provider-specific instance ID (see formats below)
 *     - Prints: "[<Tag>] Launching <cpuCores>-core/<memoryGb>GB → <instanceId>"
 *
 *   void terminate(String instanceId)
 *     - Validates instanceId not null/blank → IllegalArgumentException
 *     - Prints: "[<Tag>] Terminating <instanceId>"
 *
 *   String getProviderName()
 *     - Returns "AWS", "Azure", or "GCP"
 *
 *   double getCostPerHour(int cpuCores)
 *     - Returns provider-specific cost (see rates below)
 *
 * ── ABSTRACT PRODUCT B: ObjectStorage ───────────────────────────────────
 *
 *   void upload(String bucket, String key, String data)
 *     - Validates bucket not null/blank → IllegalArgumentException
 *     - Validates key not null/blank → IllegalArgumentException
 *     - Validates data not null → IllegalArgumentException("data must not be null")
 *     - Stores data in-memory keyed by bucket + "/" + key
 *     - Prints: "[<Tag>] Uploaded <bucket>/<key> (<data.length()> bytes)"
 *
 *   String download(String bucket, String key)
 *     - Validates bucket and key not null/blank → IllegalArgumentException
 *     - Returns the stored data, or throws NoSuchElementException(bucket+"/"+key+" not found")
 *
 *   String getProviderName()
 *     - Returns "AWS", "Azure", or "GCP"
 *
 * ── ABSTRACT PRODUCT C: ManagedDatabase ─────────────────────────────────
 *
 *   void createTable(String tableName)
 *     - Validates tableName not null/blank → IllegalArgumentException
 *     - Creates an in-memory table (list of records)
 *     - Prints: "[<Tag>] Created table '<tableName>'"
 *
 *   void insert(String tableName, String record)
 *     - Validates tableName and record not null/blank → IllegalArgumentException
 *     - Appends record to the table list
 *     - Throws IllegalStateException("Table not found: " + tableName) if table doesn't exist
 *     - Prints: "[<Tag>] INSERT into '<tableName>': <record>"
 *
 *   List<String> query(String tableName)
 *     - Validates tableName not null/blank → IllegalArgumentException
 *     - Throws IllegalStateException("Table not found: " + tableName) if table doesn't exist
 *     - Returns an UNMODIFIABLE list of all records
 *
 *   String getProviderName()
 *     - Returns "AWS", "Azure", or "GCP"
 *
 * ── ABSTRACT FACTORY: CloudProviderFactory ──────────────────────────────
 *
 *   ComputeInstance createCompute()
 *   ObjectStorage   createStorage()
 *   ManagedDatabase createDatabase()
 *
 * ── CONCRETE FAMILIES ────────────────────────────────────────────────────
 *
 *   AWS Family:
 *     AWSComputeInstance
 *       - Tag: "EC2",  ID format: String.format("aws-i-%05d", ++counter)
 *       - Cost: cpuCores * 0.048
 *     AWSS3Storage      — Tag: "S3"
 *     AWSRDSDatabase    — Tag: "RDS"
 *     AWSCloudFactory   — creates all three AWS products
 *
 *   Azure Family:
 *     AzureVMInstance
 *       - Tag: "AzureVM", ID format: String.format("azure-vm-%05d", ++counter)
 *       - Cost: cpuCores * 0.052
 *     AzureBlobStorage  — Tag: "Blob"
 *     AzureSQLDatabase  — Tag: "AzureSQL"
 *     AzureCloudFactory — creates all three Azure products
 *
 *   GCP Family:
 *     GCPComputeEngine
 *       - Tag: "GCE",  ID format: String.format("gcp-gce-%05d", ++counter)
 *       - Cost: cpuCores * 0.044
 *     GCPCloudStorage   — Tag: "GCS"
 *     GCPBigQuery       — Tag: "BigQuery"
 *     GCPCloudFactory   — creates all three GCP products
 *
 * ── CLIENT: InfrastructureDeployer ──────────────────────────────────────
 *
 *   Fields (all private final):
 *     ComputeInstance compute  — set in constructor via factory.createCompute()
 *     ObjectStorage   storage  — set in constructor via factory.createStorage()
 *     ManagedDatabase database — set in constructor via factory.createDatabase()
 *
 *   Constructor: InfrastructureDeployer(CloudProviderFactory factory)
 *     - Calls factory.createCompute(), factory.createStorage(), factory.createDatabase()
 *     - Stores results in the three fields
 *
 *   String deploy(String appName, int cpuCores, int memoryGb)
 *     - Prints: "[Deployer] Deploying '<appName>' on <compute.getProviderName()>"
 *     - Calls compute.launch(cpuCores, memoryGb) → instanceId
 *     - Calls database.createTable(appName + "_events")
 *     - Calls database.insert(appName + "_events", "Deployed: " + instanceId)
 *     - Calls storage.upload("app-configs", appName + "/latest", "instance=" + instanceId)
 *     - Prints: "[Deployer] Cost: $<getCostPerHour(cpuCores)>/hr"
 *     - Returns instanceId
 *
 *   String getProvider()
 *     - Returns compute.getProviderName()
 *
 * ── DESIGN CONSTRAINTS ──────────────────────────────────────────────────
 *   1. InfrastructureDeployer must NOT reference AWSComputeInstance, AzureVMInstance,
 *      AWSS3Storage, or any other concrete class — only interfaces.
 *   2. No if/switch on provider type anywhere in InfrastructureDeployer.
 *   3. Per-instance counters in ComputeInstance must be instance fields (NOT static).
 *   4. query() must return an unmodifiable list.
 *   5. All three products in a family must return the same getProviderName() value.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * DO NOT MODIFY the main() method — fill in the TODOs to make tests pass
 * ═══════════════════════════════════════════════════════════════════════
 */
public class CloudInfrastructurePractice {

    // =========================================================================
    // ── TODO 1: Declare the ComputeInstance interface (4 methods)
    // =========================================================================
    interface ComputeInstance {
        String launch(int cpuCores, int memoryGb);
        void terminate(String instanceId);
        String getProviderName();
        double getCostPerHour(int cpuCores);
    }


    // =========================================================================
    // ── TODO 2: Declare the ObjectStorage interface (3 methods)
    // =========================================================================
    interface ObjectStorage {
        void upload(String bucket, String key, String data);
        String download(String bucket, String key);
        String getProviderName();
    }


    // =========================================================================
    // ── TODO 3: Declare the ManagedDatabase interface (4 methods)
    // =========================================================================
    interface ManagedDatabase {
        void createTable(String tableName);
        void insert(String tableName, String record);
        List<String> query(String tableName);
        String getProviderName();
    }


    // =========================================================================
    // ── TODO 4: Declare the CloudProviderFactory interface (3 factory methods)
    // =========================================================================
    interface CloudProviderFactory {
        ComputeInstance createCompute();
        ObjectStorage   createStorage();
        ManagedDatabase createDatabase();
    }


    // =========================================================================
    // ── TODO 5: Implement AWSComputeInstance
    //            - Tag: "EC2", ID: String.format("aws-i-%05d", ++counter)
    //            - getCostPerHour: cpuCores * 0.048
    //            - getProviderName: "AWS"
    // =========================================================================
    static class AWSComputeInstance implements ComputeInstance {

        private int counter = 0;


        @Override
        public String launch(int cpuCores, int memoryGb) {
            if(cpuCores <=0 ) {
                throw new IllegalArgumentException("CPU Cores has to be > 0");
            }
            if(memoryGb <=0 ){
                throw new IllegalArgumentException("Memory in GB has to be > 0");
            }
            String instanceId = String.format("aws-i-%05d", ++counter);
            System.out.printf("[EC2] Launching %d-core/%dGB → %s%n\n", cpuCores, memoryGb, instanceId);
            return instanceId;
        }

        @Override
        public void terminate(String instanceId) {
            if(instanceId == null || instanceId.isBlank()){
                throw new IllegalArgumentException("Instance Id cannot be null or blank!!!");
            }
            System.out.println("[EC2] Terminating " + instanceId);
        }


        @Override
        public String getProviderName() {
            return "AWS";
        }
        @Override
        public double getCostPerHour(int cpuCores){
            if(cpuCores <=0){
                throw new IllegalArgumentException("CPU Cores has to be > 0");
            }
            return cpuCores * 0.048;
        }

    }


    // =========================================================================
    // ── TODO 6: Implement AWSS3Storage
    //            - Tag: "S3"
    //            - In-memory map: Map<String, String> store (key = bucket+"/"+key)
    //            - getProviderName: "AWS"
    // =========================================================================
    static class AWSS3Storage implements ObjectStorage{
        Map<String, String> storageData = new HashMap<>();
        @Override
        public void upload(String bucket, String key, String data){
            if(bucket == null || bucket.isBlank()){
                throw new IllegalArgumentException("Bucket cannot be null or blank!!");
            }
            if(key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key cannot be null or blank!!");
            }
            if(data == null || data.isBlank()) {
                throw new IllegalArgumentException("Data cannot be null or blank!!");
            }
            storageData.put(bucket + "/" + key, data);
            System.out.printf("[S3] Uploaded %s/%s (%d bytes)%n\n", bucket, key, data.length());
        }
        @Override
        public String download(String bucket, String key) {
            if(bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("Bucket cannot be null or blank");
            }
            if(key == null || key.isBlank()){
                throw new IllegalArgumentException("Key cannot be null or blank");
            }

            if(!storageData.containsKey(bucket + "/" + key)){
                throw new NoSuchElementException("bucket + key not found");
            }
            return storageData.get(bucket + "/" + key);
        }
        @Override
        public String getProviderName() {
            return "AWS";
        }
    }


    // =========================================================================
    // ── TODO 7: Implement AWSRDSDatabase
    //            - Tag: "RDS"
    //            - In-memory: Map<String, List<String>> tables
    //            - getProviderName: "AWS"
    // =========================================================================
    static class AWSRDSDatabase implements ManagedDatabase {
        Map<String, List<String>> tableData = new HashMap<>();

        @Override
        public void createTable(String tableName){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank!!");
            }
            tableData.put(tableName, new ArrayList<>());
            System.out.println("[RDS] Created table '" + tableName + "'");
        }

        @Override
        public void insert(String tableName, String record){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank or null!!");
            }
            if(record == null || record.isBlank()) {
                throw new IllegalArgumentException("Record cannot be blank or null!!");
            }
            if(!tableData.containsKey(tableName)) {
                throw new NoSuchElementException("Table Name " + tableName + " not found!!");
            }
            tableData.get(tableName).add(record);
            System.out.printf("[RDS] INSERT into '%s': %s%n\n", tableName, record);
        }

        @Override
        public List<String> query(String tableName){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank or null!!");
            }
            if(!tableData.containsKey(tableName)) {
                throw new NoSuchElementException("Table Name " + tableName + " not found!!");
            }
            return Collections.unmodifiableList(tableData.get(tableName));
        }

        @Override
        public String getProviderName() {
            return "AWS";
        }

    }


    // =========================================================================
    // ── TODO 8: Implement AWSCloudFactory
    //            - createCompute()  → return new AWSComputeInstance()
    //            - createStorage()  → return new AWSS3Storage()
    //            - createDatabase() → return new AWSRDSDatabase()
    // =========================================================================
    static class AWSCloudFactory implements CloudProviderFactory {
        @Override
        public ComputeInstance createCompute() { return new AWSComputeInstance(); }
        @Override
        public ObjectStorage createStorage() { return new AWSS3Storage(); }
        @Override
        public ManagedDatabase createDatabase() { return new AWSRDSDatabase();}
    }


    // =========================================================================
    // ── TODO 9: Implement AzureVMInstance
    //            - Tag: "AzureVM", ID: String.format("azure-vm-%05d", ++counter)
    //            - getCostPerHour: cpuCores * 0.052
    //            - getProviderName: "Azure"
    // =========================================================================
    static class AzureVMInstance implements ComputeInstance {

        private int counter = 0;


        @Override
        public String launch(int cpuCores, int memoryGb) {
            if(cpuCores <=0 ) {
                throw new IllegalArgumentException("CPU Cores has to be > 0");
            }
            if(memoryGb <=0 ){
                throw new IllegalArgumentException("Memory in GB has to be > 0");
            }
            String instanceId = String.format("azure-vm-%05d", ++counter);
            System.out.printf("[AzureVM] Launching %d-core/%dGB → %s%n\n", cpuCores, memoryGb, instanceId);
            return instanceId;
        }

        @Override
        public void terminate(String instanceId) {
            if(instanceId == null || instanceId.isBlank()){
                throw new IllegalArgumentException("Instance Id cannot be null or blank!!!");
            }
            System.out.println("[AzureVM] Terminating " + instanceId);
        }


        @Override
        public String getProviderName() {
            return "Azure";
        }
        @Override
        public double getCostPerHour(int cpuCores){
            if(cpuCores <=0){
                throw new IllegalArgumentException("CPU Cores has to be > 0");
            }
            return cpuCores * 0.052;
        }

    }

    // =========================================================================
    // ── TODO 10: Implement AzureBlobStorage
    //             - Tag: "Blob"
    //             - In-memory map: same structure as AWSS3Storage
    //             - getProviderName: "Azure"
    // =========================================================================
    static class AzureBlobStorage implements ObjectStorage{
        Map<String, String> storageData = new HashMap<>();
        @Override
        public void upload(String bucket, String key, String data){
            if(bucket == null || bucket.isBlank()){
                throw new IllegalArgumentException("Bucket cannot be null or blank!!");
            }
            if(key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key cannot be null or blank!!");
            }
            if(data == null || data.isBlank()) {
                throw new IllegalArgumentException("Data cannot be null or blank!!");
            }
            storageData.put(bucket + "/" + key, data);
            System.out.printf("[Blob] Uploaded %s/%s (%d bytes)%n\n", bucket, key, data.length());
        }
        @Override
        public String download(String bucket, String key) {
            if(bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("Bucket cannot be null or blank");
            }
            if(key == null || key.isBlank()){
                throw new IllegalArgumentException("Key cannot be null or blank");
            }

            if(!storageData.containsKey(bucket + "/" + key)){
                throw new NoSuchElementException("bucket + key not found");
            }
            return storageData.get(bucket + "/" + key);
        }
        @Override
        public String getProviderName() {
            return "Azure";
        }
    }


    // =========================================================================
    // ── TODO 11: Implement AzureSQLDatabase
    //             - Tag: "AzureSQL"
    //             - In-memory: same structure as AWSRDSDatabase
    //             - getProviderName: "Azure"
    // =========================================================================
    static class AzureSQLDatabase implements ManagedDatabase {
        Map<String, List<String>> tableData = new HashMap<>();

        @Override
        public void createTable(String tableName){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank!!");
            }
            tableData.put(tableName, new ArrayList<>());
            System.out.println("[AzureSQL] Created table '" + tableName + "'");
        }

        @Override
        public void insert(String tableName, String record){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank or null!!");
            }
            if(record == null || record.isBlank()) {
                throw new IllegalArgumentException("Record cannot be blank or null!!");
            }
            if(!tableData.containsKey(tableName)) {
                throw new NoSuchElementException("Table Name " + tableName + " not found!!");
            }
            tableData.get(tableName).add(record);
            System.out.printf("[AzureSQL] INSERT into '%s': %s%n\n", tableName, record);
        }

        @Override
        public List<String> query(String tableName){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank or null!!");
            }
            if(!tableData.containsKey(tableName)) {
                throw new NoSuchElementException("Table Name " + tableName + " not found!!");
            }
            return Collections.unmodifiableList(tableData.get(tableName));
        }

        @Override
        public String getProviderName() {
            return "Azure";
        }

    }


    // =========================================================================
    // ── TODO 12: Implement AzureCloudFactory
    //             - createCompute()  → return new AzureVMInstance()
    //             - createStorage()  → return new AzureBlobStorage()
    //             - createDatabase() → return new AzureSQLDatabase()
    // =========================================================================
    static class AzureCloudFactory implements CloudProviderFactory {
        @Override
        public ComputeInstance createCompute() { return new AzureVMInstance(); }
        @Override
        public ObjectStorage createStorage() { return new AzureBlobStorage(); }
        @Override
        public ManagedDatabase createDatabase() { return new AzureSQLDatabase();}
    }


    // =========================================================================
    // ── TODO 13: Implement GCPComputeEngine
    //             - Tag: "GCE", ID: String.format("gcp-gce-%05d", ++counter)
    //             - getCostPerHour: cpuCores * 0.044
    //             - getProviderName: "GCP"
    // =========================================================================
    static class GCPComputeEngine implements ComputeInstance {

        private int counter = 0;


        @Override
        public String launch(int cpuCores, int memoryGb) {
            if(cpuCores <=0 ) {
                throw new IllegalArgumentException("CPU Cores has to be > 0");
            }
            if(memoryGb <=0 ){
                throw new IllegalArgumentException("Memory in GB has to be > 0");
            }
            String instanceId = String.format("gcp-gce-%05d", ++counter);
            System.out.printf("[GCE] Launching %d-core/%dGB → %s%n\n", cpuCores, memoryGb, instanceId);
            return instanceId;
        }

        @Override
        public void terminate(String instanceId) {
            if(instanceId == null || instanceId.isBlank()){
                throw new IllegalArgumentException("Instance Id cannot be null or blank!!!");
            }
            System.out.println("[GCE] Terminating " + instanceId);
        }


        @Override
        public String getProviderName() {
            return "GCP";
        }
        @Override
        public double getCostPerHour(int cpuCores){
            if(cpuCores <=0){
                throw new IllegalArgumentException("CPU Cores has to be > 0");
            }
            return cpuCores * 0.044;
        }

    }


    // =========================================================================
    // ── TODO 14: Implement GCPCloudStorage
    //             - Tag: "GCS"
    //             - In-memory map: same structure
    //             - getProviderName: "GCP"
    // =========================================================================
    static class GCPCloudStorage implements ObjectStorage{
        Map<String, String> storageData = new HashMap<>();
        @Override
        public void upload(String bucket, String key, String data){
            if(bucket == null || bucket.isBlank()){
                throw new IllegalArgumentException("Bucket cannot be null or blank!!");
            }
            if(key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key cannot be null or blank!!");
            }
            if(data == null || data.isBlank()) {
                throw new IllegalArgumentException("Data cannot be null or blank!!");
            }
            storageData.put(bucket + "/" + key, data);
            System.out.printf("[GCS] Uploaded %s/%s (%d bytes)%n\n", bucket, key, data.length());
        }
        @Override
        public String download(String bucket, String key) {
            if(bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("Bucket cannot be null or blank");
            }
            if(key == null || key.isBlank()){
                throw new IllegalArgumentException("Key cannot be null or blank");
            }

            if(!storageData.containsKey(bucket + "/" + key)){
                throw new NoSuchElementException("bucket + key not found");
            }
            return storageData.get(bucket + "/" + key);
        }
        @Override
        public String getProviderName() {
            return "GCP";
        }
    }


    // =========================================================================
    // ── TODO 15: Implement GCPBigQuery
    //             - Tag: "BigQuery"
    //             - In-memory: same structure
    //             - getProviderName: "GCP"
    // =========================================================================
    static class GCPBigQuery implements ManagedDatabase {
        Map<String, List<String>> tableData = new HashMap<>();

        @Override
        public void createTable(String tableName){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank!!");
            }
            tableData.put(tableName, new ArrayList<>());
            System.out.println("[BigQuery] Created table '" + tableName + "'");
        }

        @Override
        public void insert(String tableName, String record){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank or null!!");
            }
            if(record == null || record.isBlank()) {
                throw new IllegalArgumentException("Record cannot be blank or null!!");
            }
            if(!tableData.containsKey(tableName)) {
                throw new NoSuchElementException("Table Name " + tableName + " not found!!");
            }
            tableData.get(tableName).add(record);
            System.out.printf("[BigQuery] INSERT into '%s': %s%n\n", tableName, record);
        }

        @Override
        public List<String> query(String tableName){
            if(tableName == null || tableName.isBlank()){
                throw new IllegalArgumentException("Table Name cannot be blank or null!!");
            }
            if(!tableData.containsKey(tableName)) {
                throw new NoSuchElementException("Table Name " + tableName + " not found!!");
            }
            return Collections.unmodifiableList(tableData.get(tableName));
        }

        @Override
        public String getProviderName() {
            return "GCP";
        }

    }


    // =========================================================================
    // ── TODO 16: Implement GCPCloudFactory
    //             - createCompute()  → return new GCPComputeEngine()
    //             - createStorage()  → return new GCPCloudStorage()
    //             - createDatabase() → return new GCPBigQuery()
    // =========================================================================
    static class GCPCloudFactory implements CloudProviderFactory {
        @Override
        public ComputeInstance createCompute() { return new GCPComputeEngine(); }
        @Override
        public ObjectStorage createStorage() { return new GCPCloudStorage(); }
        @Override
        public ManagedDatabase createDatabase() { return new GCPBigQuery();}
    }


    // =========================================================================
    // ── TODO 17: Implement InfrastructureDeployer (the Client)
    //             Fields (private final):
    //               ComputeInstance compute
    //               ObjectStorage   storage
    //               ManagedDatabase database
    //             Constructor:
    //               InfrastructureDeployer(CloudProviderFactory factory)
    //               - calls factory.createCompute(), createStorage(), createDatabase()
    //             Methods:
    //               String deploy(String appName, int cpuCores, int memoryGb)
    //               String getProvider()
    //
    //  IMPORTANT: InfrastructureDeployer must NOT reference any concrete class
    //             (no AWSComputeInstance, no AzureBlobStorage, etc.) — only interfaces.
    // =========================================================================
    static class InfrastructureDeployer {
        private final ComputeInstance compute;
        private final ObjectStorage storage;
        private final ManagedDatabase database;

        public InfrastructureDeployer(CloudProviderFactory factory){
            if(factory == null){
                throw new IllegalArgumentException("Cloud Provider Factory cannot be null!!");
            }
            this.compute = factory.createCompute();
            this.storage = factory.createStorage();
            this.database = factory.createDatabase();
        }

        String deploy(String appName, int cpuCores, int memoryGb) {
            if(appName == null || appName.isBlank()) {
                throw new IllegalArgumentException("AppName cannot be null or blank");
            }
            if(cpuCores <= 0){
                throw new IllegalArgumentException("CPU cores has to > 0");
            }
            if(memoryGb <= 0 ){
                throw new IllegalArgumentException("Memory has to > 0");
            }
            System.out.println("[Deployer] Deploying '" + appName + "' on " + compute.getProviderName());
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
    // DO NOT MODIFY — fill in TODOs above to make all tests pass
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

        System.out.println("\n═══ Test 4: AWS family consistency — all products return 'AWS' ═");
        CloudProviderFactory awsFactory = new AWSCloudFactory();
        String awsComputeName  = awsFactory.createCompute().getProviderName();
        String awsStorageName  = awsFactory.createStorage().getProviderName();
        String awsDatabaseName = awsFactory.createDatabase().getProviderName();
        System.out.println("Compute:  " + awsComputeName);
        System.out.println("Storage:  " + awsStorageName);
        System.out.println("Database: " + awsDatabaseName);
        boolean t4 = "AWS".equals(awsComputeName) && "AWS".equals(awsStorageName) && "AWS".equals(awsDatabaseName);
        System.out.println("Test 4 " + (t4 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 5: Azure family consistency — all return 'Azure' ═════");
        CloudProviderFactory azureFactory = new AzureCloudFactory();
        boolean t5 = "Azure".equals(azureFactory.createCompute().getProviderName())
                  && "Azure".equals(azureFactory.createStorage().getProviderName())
                  && "Azure".equals(azureFactory.createDatabase().getProviderName());
        System.out.println("Test 5 " + (t5 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 6: GCP family consistency — all return 'GCP' ══════════");
        CloudProviderFactory gcpFactory = new GCPCloudFactory();
        boolean t6 = "GCP".equals(gcpFactory.createCompute().getProviderName())
                  && "GCP".equals(gcpFactory.createStorage().getProviderName())
                  && "GCP".equals(gcpFactory.createDatabase().getProviderName());
        System.out.println("Test 6 " + (t6 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 7: ObjectStorage — upload then download ════════════════");
        ObjectStorage s3 = new AWSCloudFactory().createStorage();
        s3.upload("my-bucket", "config/app.json", "{\"env\":\"prod\"}");
        String downloaded = s3.download("my-bucket", "config/app.json");
        System.out.println("Downloaded: " + downloaded);
        System.out.println("Test 7 " + ("{\"env\":\"prod\"}".equals(downloaded) ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 8: ManagedDatabase — createTable + insert + query ═════");
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

        System.out.println("\n═══ Test 9: ManagedDatabase.query returns unmodifiable list ════");
        try {
            users.add("hacker@evil.com");
            System.out.println("Test 9 FAILED — list should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            System.out.println("Test 9 PASSED — query() returns unmodifiable list");
        }

        System.out.println("\n═══ Test 10: InfrastructureDeployer with AWS ═════════════════");
        InfrastructureDeployer awsDeployer = new InfrastructureDeployer(new AWSCloudFactory());
        String awsDeploy = awsDeployer.deploy("payment-service", 2, 8);
        System.out.println("Deployed instance: " + awsDeploy);
        boolean t10 = awsDeploy.startsWith("aws-i-") && "AWS".equals(awsDeployer.getProvider());
        System.out.println("Test 10 " + (t10 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 11: InfrastructureDeployer with Azure ══════════════");
        InfrastructureDeployer azureDeployer = new InfrastructureDeployer(new AzureCloudFactory());
        String azureDeploy = azureDeployer.deploy("payment-service", 2, 8);
        System.out.println("Deployed instance: " + azureDeploy);
        boolean t11 = azureDeploy.startsWith("azure-vm-") && "Azure".equals(azureDeployer.getProvider());
        System.out.println("Test 11 " + (t11 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 12: InfrastructureDeployer with GCP ════════════════");
        InfrastructureDeployer gcpDeployer = new InfrastructureDeployer(new GCPCloudFactory());
        String gcpDeploy = gcpDeployer.deploy("payment-service", 2, 8);
        System.out.println("Deployed instance: " + gcpDeploy);
        boolean t12 = gcpDeploy.startsWith("gcp-gce-") && "GCP".equals(gcpDeployer.getProvider());
        System.out.println("Test 12 " + (t12 ? "PASSED" : "FAILED"));

        System.out.println("\n═══ Test 13: Same deployer code, three providers (polymorphism) ═");
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
    }

    // =========================================================================
    // HINTS (read only if stuck)
    // =========================================================================

    /*
     * ── HINT 1 (Gentle) ────────────────────────────────────────────────────
     * The pattern has a 3×3 grid: 3 providers × 3 product types.
     *   - Each row = one cloud provider (AWS, Azure, GCP)
     *   - Each column = one product type (Compute, Storage, Database)
     *   - Each ConcreteFactory fills one row — it creates all products for its provider.
     *   - InfrastructureDeployer uses only column interfaces — it never sees a row.
     *
     * The key question to ask for each method: "Does this code mention a
     * concrete class name (AWSComputeInstance, etc.)?" If yes, it shouldn't
     * be in InfrastructureDeployer.
     *
     * For ObjectStorage: use a Map<String, String> keyed by bucket+"/"+key.
     * For ManagedDatabase: use a Map<String, List<String>> keyed by table name.
     *
     * ── HINT 2 (Direct) ────────────────────────────────────────────────────
     * Interface declarations:
     *
     *   interface ComputeInstance {
     *       String launch(int cpuCores, int memoryGb);
     *       void terminate(String instanceId);
     *       String getProviderName();
     *       double getCostPerHour(int cpuCores);
     *   }
     *   interface ObjectStorage {
     *       void upload(String bucket, String key, String data);
     *       String download(String bucket, String key);
     *       String getProviderName();
     *   }
     *   interface ManagedDatabase {
     *       void createTable(String tableName);
     *       void insert(String tableName, String record);
     *       List<String> query(String tableName);
     *       String getProviderName();
     *   }
     *   interface CloudProviderFactory {
     *       ComputeInstance createCompute();
     *       ObjectStorage   createStorage();
     *       ManagedDatabase createDatabase();
     *   }
     *
     * AWSComputeInstance skeleton:
     *   static class AWSComputeInstance implements ComputeInstance {
     *       private int counter = 0;
     *       @Override public String launch(int cpuCores, int memoryGb) {
     *           if (cpuCores < 1) throw new IllegalArgumentException("cpuCores must be >= 1");
     *           if (memoryGb < 1) throw new IllegalArgumentException("memoryGb must be >= 1");
     *           String id = String.format("aws-i-%05d", ++counter);
     *           System.out.printf("[EC2] Launching %d-core/%dGB → %s%n", cpuCores, memoryGb, id);
     *           return id;
     *       }
     *       @Override public void terminate(String id) {
     *           if (id == null || id.isBlank()) throw new IllegalArgumentException(...);
     *           System.out.println("[EC2] Terminating " + id);
     *       }
     *       @Override public String getProviderName() { return "AWS"; }
     *       @Override public double getCostPerHour(int cpuCores) { return cpuCores * 0.048; }
     *   }
     *
     * AWSCloudFactory:
     *   static class AWSCloudFactory implements CloudProviderFactory {
     *       @Override public ComputeInstance createCompute()  { return new AWSComputeInstance(); }
     *       @Override public ObjectStorage   createStorage()  { return new AWSS3Storage(); }
     *       @Override public ManagedDatabase createDatabase() { return new AWSRDSDatabase(); }
     *   }
     *
     * ── HINT 3 (Near-Solution) ─────────────────────────────────────────────
     * AWSS3Storage skeleton (all three ObjectStorage impls are identical except tags):
     *
     *   static class AWSS3Storage implements ObjectStorage {
     *       private final Map<String, String> store = new HashMap<>();
     *
     *       @Override
     *       public void upload(String bucket, String key, String data) {
     *           if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException(...);
     *           if (key == null || key.isBlank()) throw new IllegalArgumentException(...);
     *           if (data == null) throw new IllegalArgumentException("data must not be null");
     *           store.put(bucket + "/" + key, data);
     *           System.out.printf("[S3] Uploaded %s/%s (%d bytes)%n", bucket, key, data.length());
     *       }
     *
     *       @Override
     *       public String download(String bucket, String key) {
     *           if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException(...);
     *           if (key == null || key.isBlank()) throw new IllegalArgumentException(...);
     *           String storeKey = bucket + "/" + key;
     *           if (!store.containsKey(storeKey))
     *               throw new NoSuchElementException(storeKey + " not found");
     *           return store.get(storeKey);
     *       }
     *
     *       @Override public String getProviderName() { return "AWS"; }
     *   }
     *
     * AWSRDSDatabase skeleton:
     *   static class AWSRDSDatabase implements ManagedDatabase {
     *       private final Map<String, List<String>> tables = new HashMap<>();
     *
     *       @Override public void createTable(String name) {
     *           if (name == null || name.isBlank()) throw new IllegalArgumentException(...);
     *           tables.put(name, new ArrayList<>());
     *           System.out.println("[RDS] Created table '" + name + "'");
     *       }
     *       @Override public void insert(String table, String record) {
     *           if (!tables.containsKey(table)) throw new IllegalStateException("Table not found: " + table);
     *           tables.get(table).add(record);
     *           System.out.printf("[RDS] INSERT into '%s': %s%n", table, record);
     *       }
     *       @Override public List<String> query(String table) {
     *           if (!tables.containsKey(table)) throw new IllegalStateException("Table not found: " + table);
     *           return Collections.unmodifiableList(tables.get(table));
     *       }
     *       @Override public String getProviderName() { return "AWS"; }
     *   }
     *
     * InfrastructureDeployer:
     *   static class InfrastructureDeployer {
     *       private final ComputeInstance compute;
     *       private final ObjectStorage   storage;
     *       private final ManagedDatabase database;
     *
     *       InfrastructureDeployer(CloudProviderFactory factory) {
     *           this.compute  = factory.createCompute();
     *           this.storage  = factory.createStorage();
     *           this.database = factory.createDatabase();
     *       }
     *
     *       String deploy(String appName, int cpuCores, int memoryGb) {
     *           System.out.println("[Deployer] Deploying '" + appName + "' on " + compute.getProviderName());
     *           String id = compute.launch(cpuCores, memoryGb);
     *           database.createTable(appName + "_events");
     *           database.insert(appName + "_events", "Deployed: " + id);
     *           storage.upload("app-configs", appName + "/latest", "instance=" + id);
     *           System.out.printf("[Deployer] Cost: $%.3f/hr%n", compute.getCostPerHour(cpuCores));
     *           return id;
     *       }
     *       String getProvider() { return compute.getProviderName(); }
     *   }
     */
}
