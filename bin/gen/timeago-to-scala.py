#!/usr/bin/python3

# timeago-to-scala - convert
# https://github.com/hustcc/timeago.js/tree/master/locales to scala
#
# Copyright (C) 2017 Lakin Wecker <lakin@wecker.ca>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import os.path
import subprocess
import sys


def main(args):
    if not args:
        print("// Usage:", file=sys.stderr)
        print("// $ git clone https://github.com/hustcc/timeago.js", file=sys.stderr)
        print("// $ ./bin/gen/timeago-to-scala.py timeago.js/src/lang/*.js > modules/i18n/src/main/TimeagoLocales.scala", file=sys.stderr)
        return 1

    print("// This file is generated by bin/gen/timeago-to-scala.py.")
    print("// Do not edit it manually!")
    print()
    print("package lila.i18n")
    print()
    print("object TimeagoLocales {")
    print("  val js: Map[String, String] = Map(")

    first = True

    for arg in sorted(args, key=os.path.basename):
        if not arg.endswith(".js") or arg.endswith("index.js"):
            print("    // Skipping file: {}".format(arg), file=sys.stderr)
            continue

        locale = os.path.basename(arg).replace(".js", "")

        if first:
            first = False
        else:
            print(",")

        if locale == "en_short":
            print("    // Hardcoded locale: en", file=sys.stderr)
            print('''    "en" -> """lichess.timeagoLocale=function(s,n){return[["just now","right now"],["%s seconds ago","in %s seconds"],["1 minute ago","in 1 minute"],["%s minutes ago","in %s minutes"],["1 hour ago","in 1 hour"],["%s hours ago","in %s hours"],["1 day ago","in 1 day"],["%s days ago","in %s days"],["1 week ago","in 1 week"],["%s weeks ago","in %s weeks"],["1 month ago","in 1 month"],["%s months ago","in %s months"],["1 year ago","in 1 year"],["%s years ago","in %s years"]][n]};"""''', end="")
            continue

        print("    // {} -> {}".format(arg, locale), file=sys.stderr)

        with open(arg) as f:
            js = postprocess(terser(preprocess(f.read())))
            print('''    "{}" -> """{}"""'''.format(locale, js), end="")

    print()
    print("  )")
    print("}")

    return 0


def terser(js):
    p = subprocess.Popen(["yarn", "run", "--silent", "terser", "--mangle", "--compress", "--safari10"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=sys.stderr)
    stdout, stderr = p.communicate(js.encode("utf-8"))
    if p.returncode != 0:
        sys.exit(p.returncode)
    return stdout.decode("utf-8")


def preprocess(js):
    return js.replace("export default function", "lichess.timeagoLocale=function");

def postprocess(js):
    return "(function(){" + js.strip() + "})()"



if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
