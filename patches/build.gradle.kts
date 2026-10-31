group = "app.morphe"

patches {
    about {
        name = "Mobile Stream Patches"
        description = "Ad-blocking and anti-tracking patches for phone streaming apps"
        source = "https://github.com/semicoloncolonel/mobile-stream-patches"
        author = "semicoloncolonel"
        contact = "https://github.com/semicoloncolonel"
        website = "https://github.com/semicoloncolonel/mobile-stream-patches"
        license = "GPLv3"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
