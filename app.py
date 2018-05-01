import random
import subprocess
from pathlib import Path

from flask import Flask, request, make_response

app = Flask(__name__)
root = Path(__name__).parent.resolve()


def decode_url(u):
    if u.startswith('xn--'):
        return u[4:].encode('ascii').decode('punycode')
    return u


@app.route('/')
def index():
    host = request.environ['HTTP_HOST']
    parts = map(decode_url, host.split('.')[:-2])
    text = '\n'.join([_.upper().replace('-', ' ') for _ in parts] + ['SALOPE'])

    fonts = list((root / 'fonts').iterdir())
    font = random.choice(fonts)
    images = list((root / 'images').glob('*.jpg'))
    image = random.choice(images)

    p = subprocess.Popen(
        ['/usr/lib/jvm/java-8-openjdk-amd64/bin/java',
         '-jar', str(root / 'inspirational-1.0.jar'),
         '-i', image, '-f', font, '-w', '1920', '-h', '1080',
         '-q', '.75', '-t', text],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    return make_response((p.stdout.read(), 200, {'content-type': 'image/jpeg'}))


application = app

if __name__ == '__main__':
    app.run(debug=True)

