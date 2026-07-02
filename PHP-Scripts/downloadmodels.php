<?php
ini_set('display_errors', '1');
ini_set('display_startup_errors', '1');
error_reporting(E_ALL);
// download the trained model to the watch
include 'silentsecret.php';
require_once 'vendor/autoload.php';

use Couchbase\ClusterOptions;
use Couchbase\Cluster;

$connectionString = "couchbase://localhost";
$options = new ClusterOptions();

$options->credentials("Administrator", $key);
$cluster = new Cluster($connectionString, $options);

$bucket = $cluster->bucket('smart-fall-blobs');
$docid = isset($_POST['docid']) ? $_POST['docid'] : 'default';

$query = "SELECT fpaths, version, best_threshold, parts FROM `smart-fall-blobs` WHERE META().id = '$docid'";

$res = $cluster->query($query);
foreach ($res->rows() as $row) {
    foreach ($row['parts'] as $x => $x_value) {
        $subquery = "SELECT weight, blob FROM `smart-fall-blobs` WHERE META().id = '$x_value'";
        $subres = $cluster->query($subquery);
        foreach ($subres->rows() as $subrow) {
            var_export($row['fpaths'][$x]);
            echo("comma");
            var_export($row['version']);
            echo("comma");
            var_export($subrow['blob']);
            echo("comma");
            var_export($subrow['weight']);
            echo("comma");
            var_export($row['best_threshold']);
            echo("split");
        }
    }
}

?>
