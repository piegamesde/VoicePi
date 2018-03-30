#! /usr/bin/python3
from gi import pygtkcompat
import gi
import argparse
import sys
import time
from threading import Thread
import os

gi.require_version('Gst', '1.0')
from gi.repository import GObject, Gst
GObject.threads_init()
Gst.init(None)
    
gst = Gst
    
print("Using pygtkcompat and Gst from gi")

pygtkcompat.enable() 
pygtkcompat.enable_gtk(version='3.0')

import gtk

class DemoApp(object):
    """GStreamer/PocketSphinx Demo Application"""
    def __init__(self, dic=None, lm=None):
        """Initialize the speech components"""
        sys.stdout.write("Starting\n")
        self.pipeline = gst.parse_launch('autoaudiosrc ! audioconvert !  audioresample ! pocketsphinx name=asr ! fakesink')
        bus = self.pipeline.get_bus()
        bus.add_signal_watch()
        bus.connect('message::element', self.element_message)

        asr=self.pipeline.get_by_name('asr')
        asr.set_property('lm', lm)
        asr.set_property('dict', dic)

        self.pipeline.set_state(gst.State.PLAYING)
        sys.stdout.flush()


    def element_message(self, bus, msg):
        """Receive element messages from the bus."""
        msgtype = msg.get_structure().get_name()
        if msgtype != 'pocketsphinx':
            return

        if msg.get_structure().get_value('final'):
            self.final_result(msg.get_structure().get_value('hypothesis'),
                msg.get_structure().get_value('confidence'))
        elif msg.get_structure().get_value('hypothesis'):
            self.partial_result(msg.get_structure().get_value('hypothesis'))
        sys.stdout.flush()

    def partial_result(self, hyp):
        """Delete any previous selection, insert text and select it."""
        sys.stdout.write("Partial:"+hyp+"\n")

    def final_result(self, hyp, confidence):
        """Insert the final result."""
        sys.stdout.write("Final:"+hyp+"\n")


parser = argparse.ArgumentParser(description='Process some integers.')
parser.add_argument('--dic', nargs='?')
parser.add_argument('--lm', nargs='?')
args = vars(parser.parse_args())
app = DemoApp(args['dic'], args['lm'])

timestamp = time.time()

def check_parent():
    while time.time() - timestamp < 20:
        time.sleep(10)
    app.pipeline.set_state(gst.State.PAUSED)
    sys.stdout.write("Done")
    sys.stdout.flush()
    os._exit(0)

def check_input():
    for line in sys.stdin:
        sys.stdout.write("Read:"+line)
        if line == "Quit":
            break;
        if line == "Play":
            sys.stdout.write("Play")
            app.pipeline.set_state(gst.State.PLAYING)
        if line == "Pause":
            sys.stdout.write("Pause")
            app.pipeline.set_state(gst.State.PAUSED)
        timestamp = time.time()
        sys.stdout.flush()
    app.pipeline.set_state(gst.State.PAUSED)
    sys.stdout.write("Done")
    sys.stdout.flush()
    os._exit(0)

thread1 = Thread(target = check_parent)
thread1.start()
thread2 = Thread(target = check_input)
thread2.start()

gtk.main()

