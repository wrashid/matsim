var fs = require( 'fs' );
var path = require( 'path' );
var util = require('util');
var child_process = require('child_process');

var currentDir = ".";

fs.readdir( currentDir, function(err, files ) {
    if( err ) {
        console.error( "Could not list the directory.", err );
        process.exit( 1 );
    }

    files.forEach( function( file, index ) {

        var fromPath = path.join( currentDir, file );

        fs.stat( fromPath, function( error, stat ) {
            if( error ) {
                console.error( "Error stating file.", error );
                return;
            }

            if( stat.isDirectory() ) {
                if (file == "_template") return;
                console.log( "'%s' is a contrib.", fromPath );
                cmd = util.format("mvn deploy:deploy-file -Durl=https://api.bintray.com/maven/matsim/matsim/matsim -DrepositoryId=bintray -Dfile=%s -DpomFile=%s -Dclassifier=sources", path.join(fromPath, util.format("target/%s-0.8.0-sources.jar", file)), path.join(fromPath, "pom.xml"));
                console.log(cmd);
                child_process.exec(cmd, function(error, stdout, stderr) {
                    // command output is in stdout
                    console.log(stdout);
                    console.error(stderr);
                });
            }

        } );
    } );
} );