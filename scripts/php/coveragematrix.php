<?php
$patients = $_POST['patients'];
$analysis = $_POST['analysis'];
$mapqv = $_POST['mapqv'];
$output = $_POST['output'];
$bed = $_FILES['bed']['tmp_name'];
#echo "<pre>"; 
#	print_r($_FILES); 
#echo "</pre>"; 

# Using Slurm jobs
#$cmd = "/data/highlander/do_coverageMatrix.sh $bed $patients $analysis $mapqv $output";
$bedcp = "/data/highlander/bam/bamout/".basename($bed);
copy($bed,$bedcp);
$now = date('Y')."-".date('m')."-".date('d').".".date('H')."-".date('i');
$cmd = "sbatch --parsable --job-name=CovMat.$now --partition=computeGPU --chdir=/tmp --mail-user=simon.boutry@uclouvain.be --mail-type=FAIL,REQUEUE --ntasks=1 --cpus-per-task=4 --mem=14g --time=500:00:00 --output=/data/highlander/php/logs/%j.%x --export=ALL,bed=$bedcp,patients=$patients,analysis=$analysis,mapqv=$mapqv,outputfile=$output /data/highlander/do_coverageMatrix.sh";

echo "$now\r\n";
echo "$bedcp\r\n";
echo "$patients\r\n";
echo "$analysis\r\n";
echo "$mapqv\r\n";
echo "$output\r\n";
echo "*cmd^$cmd*\r\n";
ob_implicit_flush(true);
ob_end_flush();
$descriptorspec = array(
   0 => array("pipe", "r"),   // stdin is a pipe that the child will read from
   1 => array("pipe", "w"),   // stdout is a pipe that the child will write to
   2 => array("pipe", "w")    // stderr is a pipe that the child will write to
);
flush();

$process = proc_open($cmd, $descriptorspec, $pipes, realpath('./'), array());
stream_set_blocking($pipes[2], 0);
if (is_resource($process)) {
    #has to use 'false !==' because if the character '0' is read, it's considered false and the php script exits or hangs forever
    while (false !== ($s = fgetc($pipes[1]))) {
        print $s;
        #ob_flush(); #erreur dans log apache = ob_flush(): failed to flush buffer.
        flush();
    }
}

echo "Errors:\r\n";
fclose($pipes[1]);
echo stream_get_contents($pipes[2]);
echo "\r\n";
fclose($pipes[2]);
$exit=proc_close($process);
echo "*exitcode^$exit*";
?>
