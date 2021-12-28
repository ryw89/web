#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys
import tempfile


class OrgToHtml():
    def __init__(self, infile, outfile):
        self.infile = infile
        self.outfile = outfile

    @property
    def html_file(self):
        return self.infile.replace('.org', '.html')

    def build_script(self):
        tmp = tempfile.mkstemp()[1]
        elisp = f'''(find-file "{self.infile}")
                 (org-html-export-to-html)'''

        with open(tmp, 'w') as f:
            f.write(elisp)

        print(f'Wrote script to {tmp}.')

        self.script_path = tmp

    def execute_script(self):
        cmd = ['emacs', '--script', self.script_path]
        ps = subprocess.Popen(cmd,
                              shell=False,
                              stdout=subprocess.PIPE,
                              stderr=subprocess.PIPE)

        out, err = ps.communicate()
        exit_code = ps.poll()

        if out:
            out = out.decode()
        if err:
            err = err.decode()

        self.stdout, self.stderr = out, err
        self.exit_code = ps.poll()

        if self.exit_code != 0:
            raise RuntimeError(f'org2html failed with error:\n{self.stderr}')

    def write_out(self):
        shutil.copyfile(self.html_file, self.outfile)


if __name__ == '__main__':
    oth = OrgToHtml(sys.argv[1], sys.argv[2])
    oth.build_script()
    oth.execute_script()
    oth.write_out()

    print(f'Wrote {oth.outfile}.')
