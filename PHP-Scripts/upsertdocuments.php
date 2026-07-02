<?php
ini_set('display_errors', '1');
ini_set('display_startup_errors', '1');
error_reporting(E_ALL);

include 'silentsecret.php';
require_once 'vendor/autoload.php';

use Couchbase\ClusterOptions;
use Couchbase\Cluster;

try{
	$connectionString = "couchbase://localhost";
	$options = new ClusterOptions();

	$options->credentials("Administrator", $key);
	$cluster = new Cluster($connectionString, $options);
	
	$data = file_get_contents('php://input');
	$jsons = json_decode($data);
	$bucketName = $jsons[0]->bucket;
	$documents = $jsons[0]->documents;
	if(!is_array($documents)){
		$documents = json_decode($documents);
	}

	$bucket = $cluster->bucket($bucketName);
	$scope = $bucket->scope('_default');

	foreach ($documents as $document) {
		$id = $document->id;
		$keys = $document->keys;
		$upsertResult = $scope->collection('_default')->upsert($id, $keys);
	}
}
catch (Exception $e) {
	echo 'ERROR Message: ' .$e->getMessage();
}
?>
