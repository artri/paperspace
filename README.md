# paper{s}pace

a small web application to manage all your offline documents. 
Provides a searchable storage for your documents and reminds you of upcoming com.dedicatedcode.paperspace.feeder.tasks.

**start page** 
![starting page](https://gitlab.com/dedicatedcode/paperspace/-/wikis/uploads/db7a4422094235adb46bb9e933a5f507/main.png)

**task view** 
![task view](https://gitlab.com/dedicatedcode/paperspace/-/wikis/uploads/fef4451040a37306a4eaff7dc92013cd/task-view.png)

**search results** 
![search results](https://gitlab.com/dedicatedcode/paperspace/-/wikis/uploads/3ce55e7710aa8b15109bcf794962cde0/search-results.png)

More screenshots of the current state can be found in the [wiki](https://gitlab.com/dedicatedcode/paperspace/-/wikis/Screenshots)

## Introduction
what can it do:
- provides a searchable storage for your documents 
- easy to install and use
- stores automatically documents or com.dedicatedcode.paperspace.feeder.tasks placed in specific folders
- com.dedicatedcode.paperspace.feeder.tasks have a due date associated, and you will be reminded before the date is approaching by mail

what is it **not** (and probably never be)
- not a full sized business application
- means:
  - no user management
  - no settings in the ui
  
## use case
I was getting tired of having to store all my offline documents and then can´t find the one I need right now. 
After trying multiple solutions for this problem and not finding a simple one I decided to tackle that on my own.

So my workflow with paper{s}pace:
- scan a document with my [Brother-ADS-1100W](https://www.amazon.de/Brother-ADS-1100W-Dokumentenscanner-Duplex-schwarz/dp/B00GHUCKBY) which uploads it via ftp in either the documents- or com.dedicatedcode.paperspace.feeder.tasks-folder.
- get a mail when the processing is done and i can open the document directly from my browser and print it or add some metadata. 
- if it is a task, then the app will remind me when the task needs attention (I´m terrible at remembering that stuff).

Inspired by
- [Paperless](https://github.com/the-paperless-project/paperless) 
- [Mayan EDMS](https://www.mayan-edms.com/) 

## documentation and installation instructions

Documentation of paper{s}pace and installation instructions can be found on [dedicatedcode.com](https://www.dedicatedcode.com/projects.html)

