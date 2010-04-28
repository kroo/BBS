var http = require('http'),
    libxml = require('libxml');

function parseURL(degree) {
  var seed = http.createClient(80, 'seed.ucsd.edu');
  var request = seed.request('GET', '/labelme/index2.php?folder=room_scans_'+degree, {'host': 'seed.ucsd.edu'});
  request.addListener('response', function (response) {
  response.setEncoding('utf8');
    response.addListener('data', function (chunk) {
      var doc = libxml.parseXmlString(chunk);
      var script = doc.find("/html/body/table//script");
      sys.puts("got " + script.length + " elements");
    });
  });
  request.end();
}
