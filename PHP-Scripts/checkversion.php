<?php
//This script is used to check where is there is better model trained for the 
//user with a particular UUID. The isBest is true if there is a better model.

include 'silentsecret.php';
require_once 'vendor/autoload.php';

use Couchbase\ClusterOptions;
use Couchbase\Cluster;

$connectionString = "couchbase://localhost";
$options = new ClusterOptions();

$options->credentials("Administrator", $key);
$cluster = new Cluster($connectionString, $options);

$bucket = $cluster->bucket('smart-fall-blobs');

$uuid = isset($_POST['uuid']) ? $_POST['uuid'] : 'default';

$query = "SELECT META().id FROM `smart-fall-blobs` WHERE uuid = '$uuid' AND isBest = True ORDER BY version DESC LIMIT 1";
$result = $cluster->query($query);

foreach ($result->rows() as $row) {
    var_export($row['id']);
}

?>
