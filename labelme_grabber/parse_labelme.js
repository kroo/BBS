var http = require('http'),
    libxml = require('./libxmljs'),
    fs = require('fs'),
    path = require('path'),
    spawn = require('child_process').spawn,
    sys = require('sys'),
    url = require('url'),
    assert = require('assert');

var seed = http.createClient(80, 'seed.ucsd.edu');
function parseURL(degree) {
  var request = seed.request('GET', '/labelme/index2.php?folder=room_scans_'+degree, {'host': 'seed.ucsd.edu'});
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
      handleMovie(movie, degree);
    });
  });
  request.end();
}

/**
 * once we have grabbed the movie array from the roomscans.php file, we need to
 * filter out the pages that have useful polygons on them.  From there, we
 * download all of them, and further filter the polygons by creator.
 */
function handleMovie(movie, angle) {
  nannot = 0;
  npolys = 0;
  movie.forEach(function(imagearray, index) {
    var thumb_url = imagearray[0],
        image_info = imagearray[1],
        unused_js_call = imagearray[2];
    var image_name = imagearray[1].split(", ")[0];
    var name = image_name.split(".")[0];
    var annocount = parseInt(imagearray[1].split("annocount=")[1]);
    if(annocount > 0) {
      var image_url = ("http://seed.ucsd.edu/labelme/Images/room_scans_" + angle + "/" + image_name);
      var path_url = ("http://seed.ucsd.edu/labelme/Annotations/room_scans_" + angle + "/" +
                        name + ".xml");
      save_from_url(angle, name, image_url);
      save_from_url(angle, name, path_url);

      extract_polys(angle, name, path_url);
      nannot++;
      npolys += annocount;
    }
  });
  sys.debug("Statistics for angle " + angle + ":");
  sys.debug("total images:            " + movie.length);
  sys.debug("total annotated images:  " + nannot);
  sys.debug("total annotated polgons: " + npolys);
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

function extract_polys(angle, name, urlstr) {
  var url_info = url.parse(urlstr);
  assert.equal(url_info.host, "seed.ucsd.edu", "assuming all polygon urls are hosted on seed, for now.");

  var request = seed.request('GET', url_info.pathname, {'host' : 'seed.ucsd.edu'}); 

  request.addListener('response', function (response) {
    response.setEncoding('utf8');
    var data = "";
    response.addListener('data', function (chunk) { data += chunk; });
    response.addListener('end', function() {
      var doc;
      try {
        doc = libxml.parseXmlString(data);
      } catch(e) {
        sys.debug("parsing doc failed: " + data);
        return;
      }
      var annotation = doc.find("/annotation")[0];
      var objects = doc.find("/annotation/object");
      sys.puts("found objects: " + objects.length);
      var obj_types = {};
      objects.forEach(function(obj) {
        var type = obj.get("name").text();
        if(!(obj_types[type])) { obj_types[type] = []; }
        obj_types[type].push(obj);
        obj.remove();
      });
      
      for(var type in obj_types) {
        sys.debug("found type " + type);
	obj_types[type].forEach(function(obj) { annotation.addChild(obj) });
  	var text_str_loc = ""+doc.toString();
        var txtname_loc = type + ".xml";
	sys.debug("prepping for " + txtname_loc);
	(function(txtname, text_str) {
	  prepare_for_save(angle, name, function(imgdir) {
	    var image_dir = imgdir;
	    var text_filename = image_dir + "/" + txtname;
	    sys.debug("writing to " + text_filename);
            fs.open(text_filename, 'w', 0777, function(err, fd) {
              fs.write(fd, text_str, null, 'utf8', function() {
	        sys.debug("script written to " + txtname);
	      });
	    });
	  });
	})(txtname_loc, text_str_loc);
	obj_types[type].forEach(function(obj) { obj.remove() });
      }
    });
  });
  request.end();
}

[-90, -67, -45, -22, 0, 22, 45, 67, 90].forEach(function(num) {
  parseURL(num);
});
