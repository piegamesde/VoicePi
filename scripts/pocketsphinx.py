#! /usr/bin/python3
from gi import pygtkcompat
import gi
import argparse
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
        print("Starting")
        self.pipeline = gst.parse_launch('autoaudiosrc ! audioconvert !  audioresample ! pocketsphinx name=asr ! fakesink')
        bus = self.pipeline.get_bus()
        bus.add_signal_watch()
        bus.connect('message::element', self.element_message)

        asr=self.pipeline.get_by_name('asr')
        asr.set_property('lm', lm)
        asr.set_property('dict', dic)

        self.pipeline.set_state(gst.State.PLAYING)


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

    def partial_result(self, hyp):
        """Delete any previous selection, insert text and select it."""
        print("Partial:"+hyp)

    def final_result(self, hyp, confidence):
        """Insert the final result."""
        print("Final:"+hyp)


parser = argparse.ArgumentParser(description='Process some integers.')
parser.add_argument('--dic', nargs='?')
parser.add_argument('--lm', nargs='?')
args = vars(parser.parse_args())
app = DemoApp(args['dic'], args['lm'])
gtk.main()

