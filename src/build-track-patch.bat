@echo off
cd
@echo on
set TRACK=bullring
del %TRACK%.zip %TRACK%.sra
7z a -bb1 -tzip -r %TRACK% %TRACK%.json %TRACK%-*
rename %TRACK%.zip %TRACK%.sra
pause