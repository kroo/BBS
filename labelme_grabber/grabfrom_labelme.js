var http = require('http'),
    libxml = require('./libxmljs'),
    fs = require('fs'),
    path = require('path'),
    spawn = require('child_process').spawn,
    sys = require('sys'),
    url = require('url'),
    assert = require('assert');

var seed = http.createClient(80, 'seed.ucsd.edu');
function parseURL(folder) {
  var request = seed.request('GET', '/labelme/index2.php?folder='+folder, {'host': 'seed.ucsd.edu'});
  request.addListener('response', function (response) {
    response.setEncoding('utf8');
    var data = "";
    response.addListener('data', function (chunk) {
      data += chunk;
    });
    response.addListener('end', function() {
      var doc;
      try {
        doc = libxml.parseXmlString(data);
      } catch(e) {
        sys.debug("parsing doc failed: " + e);
        return;
      }
      var script = doc.find("/html/body/table//script");
      script = script[0].text();
      script = ("(function(photogallery) {\n" + script + "\nreturn movie;\n})(Object);\n");
/*      var script_filename = "./labelme_js_code.js";
      fs.open(script_filename, 'w', 0777, function(err, fd) {
        fs.write(fd, script, null, 'utf8', function() {
	  sys.debug("script written to " + script_filename);
	  });
	});
*/
      var movie = eval(script);
      handleMovie(movie, folder);
    });
  });
  request.end();
}

/**
 * once we have grabbed the movie array from the roomscans.php file, we need to
 * filter out the pages that have useful polygons on them.  From there, we
 * download all of them, and further filter the polygons by creator.
 */
function handleMovie(movie, folder_name) {

  movie.forEach(function(imagearray, index) {
    var thumb_url = imagearray[0],
        image_info = imagearray[1],
        unused_js_call = imagearray[2];
    var image_name = imagearray[1].split(", ")[0];
    var name = image_name.split(".")[0];

    var image_url = ("http://seed.ucsd.edu/labelme/Images/" + folder_name + "/" + image_name);
    save_from_url(folder_name, name, image_url);

  });
  sys.debug("Statistics for folder_name " + folder_name + ":");
  sys.debug("total images:            " + movie.length);
}

function prepare_for_save(angle, name, callback) {
  var angle_dir = "./img_" + angle;
  path.exists(angle_dir, function(angle_dir_exists) {
    sys.debug("angle_dir exists? " + angle_dir_exists)
    if(!angle_dir_exists) { try { fs.mkdirSync(angle_dir, 0777); } catch(e) {} }
    var img_dir = angle_dir + "/" + name;
    path.exists(img_dir, function(img_dir_exists) {
      sys.debug("img_dir exists? " + img_dir_exists + ": " + img_dir)
      if(!img_dir_exists) { try { fs.mkdirSync(img_dir, 0777); } catch(e) {} }
      callback(img_dir);
    });
  });
}

function save_from_url(angle, name, url) {
  prepare_for_save(angle, name, function(img_dir) {
    var wget = spawn('wget', ['-P', img_dir, url]);
    wget.addListener('exit', function(code) {
      sys.debug(url + " saved with status " + code); 
    });
  });
}

[1263253242, 1263487603, 1263334243, 1263415031, 1263334930].forEach(function(num) {
  parseURL(num);
});
