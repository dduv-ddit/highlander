<?php

// Set a valid header so browsers pick it up correctly.
header('Content-type: text/html; charset=utf-8');
// Explicitly disable caching so Varnish and other upstreams won't cache.
header("Cache-Control: no-cache, must-revalidate");
// Setting this header instructs Nginx to disable fastcgi_buffering and disable gzip for this request.
header('X-Accel-Buffering: no');
// Turn off output buffering
ini_set('output_buffering', 'off');
// Turn off PHP output compression
ini_set('zlib.output_compression', false);
// Implicitly flush the buffer(s)
ini_set('implicit_flush', true);
ob_implicit_flush(true);
// Clear, and turn off output buffering
while (ob_get_level() > 0) {
    // Get the curent level
    $level = ob_get_level();
    // End the buffering
    ob_end_clean();
    // If the current level has not changed, abort
    if (ob_get_level() == $level) break;
}
// Disable apache output buffering/compression
if (function_exists('apache_setenv')) {
    apache_setenv('no-gzip', '1');
    apache_setenv('dont-vary', '1');
}
// Try to stop buffering
ob_end_flush();


$filename = $_POST['filename'];
$patients = $_POST['patients'];
$positions = $_POST['positions'];

//$cmd = "/data/highlander/do_bamcheck.sh 99999 \"panels_torrent_caller|VA-1159-T.pVMGENES;panels_torrent_caller|VA-1167-T.pVMGENES;panels_torrent_caller|VA-1179-T.pVMGENES;panels_torrent_caller|VA-1192-T.pVMGENES;panels_torrent_caller|VA-1206-T.pVMGENES;panels_torrent_caller|VA-1211-T.pVMGENES;panels_torrent_caller|VA-1217-T.pVMGENES;panels_torrent_caller|VA-1218-T.pVMGENES;panels_torrent_caller|VA-1230-T.pVMGENES;panels_torrent_caller|VA-1246-T.pVMGENES;panels_torrent_caller|VA-1249-T.pVMGENES;panels_torrent_caller|VA-1250-T.pVMGENES\" \"9:27206638;9:27212707;9:27212770\"";
$cmd = "/data/highlander/do_bamcheck.sh $filename $patients $positions";
// Still buffering issues, fill the buffer with spaces
$string = str_repeat('#', 4096 - strlen("*cmd^$cmd*") );
echo $string;
echo "*cmd^$cmd*";
flush();

$descriptorspec = array(
   0 => array("pipe", "r"),   // stdin is a pipe that the child will read from
   1 => array("pipe", "w"),   // stdout is a pipe that the child will write to
   2 => array("pipe", "w")    // stderr is a pipe that the child will write to
);
$process = proc_open($cmd, $descriptorspec, $pipes, realpath('./'), array());
if (is_resource($process)) {
    #has to use 'false !==' because if the character '0' is read, it's considered false and the php script exits or hangs forever
    while (false !== ($s = fgetc($pipes[1]))) {
        // Still buffering issues, fill the buffer with spaces
        $string = str_repeat('#', 4096);
        echo $string;
        print $s;
        flush();
    }
}
fclose($pipes[1]);
$exit=proc_close($process);
echo "*exitcode^$exit*";
?>
