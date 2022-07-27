<?php
$analysis = $_POST['analysis'];
$reference = $_POST['reference'];
$sample = $_POST['sample'];
$chr = $_POST['chr'];
$pos = $_POST['pos'];
$normal = $_POST['normal'];
$cmd = "/data/highlander/do_bamout.sh $analysis $reference $sample $chr $pos $normal";
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
?>
