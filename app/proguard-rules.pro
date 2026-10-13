# WorkManager instantiates workers by their class name after a process restart.
-keep class com.hananakick.arrrr.sync.FeedSyncWorker { public <init>(...); }
