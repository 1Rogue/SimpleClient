SimpleClient
============

## Table of contents

 * __[Preparing a version for use](#prepare)__
 * __[Using SimpleClient](#usage)__
 * __[Noted and unverifiable bugs](#bugs)__

## <a name="prepare"></a>Preparing a version for use

Before doing anything, you will need to make sure that the natives are available for
SimpleClient to make use of. In the future, this may no longer be necessary, but for now
it's simple a step that needs to be taken.

To make the natives available, launch minecraft normally as you wish, and then open the
relevant folder for your current version. This should be located under somewhere such as
`.minecraft/versions/1.7.9` or any variation. In this folder *while the client is running*,
you should see a folder along the lines of `natives-######...`, simply copy the contents of
that folder into a new folder named `natives`. This `natives` folder should be in the same
directory as the `natives-######...`.

## <a name="usage"></a>Using SimpleClient

Using SimpleClient in your code can be as simple and painless as:

```java
new SimpleClient("1.7.9", "username", "password", new File(System.getenv("APPDATA"))).openMinecraft();
```

Of course, there are a couple of things outside of this to be aware of, but that's the main thing really.

There are two ways to construct a SimpleClient instance. Once is supplying the four necessary fields
directly, as shown above. The other involves passing a scanner instance for requesting input. This can
be done with `stdin` as shown below:

```java
public static void main(String[] args) throws IOException {
    Scanner scan = new Scanner(System.in);
    new SimpleClient(scan).openMinecraft();
    System.exit(0);
}
```
And that is really the jist of it all.

**Do note, however**, that opening the minecraft client creates a subproccess and that your main process
(the one launching the client) will not close unless you specifically call `System#exit(int)`.

These are the parameters for the constructor:

 * [0] -> Version
 * [1] -> Username
 * [2] -> Password
 * [3] -> a File instance representing the folder that contains the .minecraft folder.

## <a name="bugs"></a>Noted and unverifiable bugs

* Skins don't always show up
* The client logo is different
