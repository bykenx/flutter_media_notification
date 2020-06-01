import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_media_notification/flutter_media_notification.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String status = 'hidden';

  Duration position = Duration.zero;
  Duration duration = Duration(seconds: 300);
  double rate = 2.0;

  Timer timer;

  @override
  void initState() {
    super.initState();

    MediaNotification.setListener('pause', () {
      timer?.cancel();
      setState(() => status = 'pause');
    });

    MediaNotification.setListener('play', () {
      startTimer();
      setState(() => status = 'play');
    });

    MediaNotification.setListener('next', () {});

    MediaNotification.setListener('prev', () {});

    MediaNotification.setListener('select', () {});

    startTimer();
  }

  startTimer() {
    timer?.cancel();
    timer = Timer.periodic(Duration(seconds: 1), (timer) {
      if (position >= duration) {
        timer.cancel();
      }
      setState(() {
        position += Duration(seconds: 1);
      });
    });
  }

  clearTimer() {
    timer?.cancel();
  }

  getFormatTime(Duration duration) {
    return '${duration.inMinutes.toString().padLeft(2, '0')}:${(duration.inSeconds % 60).toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin example app'),
        ),
        body: new Center(
            child: Container(
          height: 250.0,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Text('${getFormatTime(position)}/${getFormatTime(duration)}')
                ],
              ),
              FlatButton(
                  child: Text('Show notification'),
                  onPressed: () {
                    MediaNotification.showNotification(
                        title: 'Title', author: 'Song author', cover: 'https://files.bookce.gambition.cn/Fiuo-lkeVPjtv9WIolstEmP7E25B');
                    MediaNotification.updatePlaybackInfo(
                        position: position,
                        duration: duration,
                        rate: 1.0,
                    );
                    setState(() => status = 'play');
                  }),
              FlatButton(
                  child: Text('Update notification'),
                  onPressed: () {
                    MediaNotification.showNotification(
                        title: 'New Title',
                        author: 'New Song author',
                        isPlaying: false);
                    setState(() => status = 'pause');
                  }),
              FlatButton(
                  child: Text('Hide notification'),
                  onPressed: () {
                    MediaNotification.hideNotification();
                    setState(() => status = 'hidden');
                  }),
              Text('Status: ' + status)
            ],
          ),
        )),
      ),
    );
  }
}
