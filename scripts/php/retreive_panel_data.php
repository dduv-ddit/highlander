<?php
$sequencer = $_POST['sequencer'];
$run = $_POST['run'];
$library = $_POST['library'];
$project = $_POST['project'];
$sample = $_POST['sample'];
$cmd = "/data/highlander/retreive_panel_data.sh -S $sequencer -p $run -l $library -P $project -L $sample";
echo "$cmd\n";
ob_implicit_flush(true);
ob_end_flush();
$descriptorspec = array(
   0 => array("pipe", "r"),   // stdin is a pipe that the child will read from
   1 => array("pipe", "w"),   // stdout is a pipe that the child will write to
   2 => array("pipe", "w")    // stderr is a pipe that the child will write to
);
flush();
$process = proc_open($cmd, $descriptorspec, $pipes, realpath('./'), array());
if (is_resource($process)) {
    #has to use 'false !==' because if the character '0' is read, it's considered false and the php script exits or hangs forever
    while (false !== ($s = fgetc($pipes[1]))) {
        print $s;
        flush();
    }
}
fclose($pipes[1]);
$exit=proc_close($process);
echo "\nexitcode $exit\n";

### ATTENTION: when retreive_panel_data.sh is launched from www-data user (php), it tries to add host verification to /var/www/.ssh/known_hosts the first time it connects to it, but doesn't seem to have the permission to do so. So I connected to PROTON using ssh with highlander user and copy the content of highlander's known_hosts to /var/www/.ssh/known_hosts. Probably necessary to do it again with ION_TORRENT (when it's online), MiSeq (when we finally receive it) or if the host information changes in any way ###

?>