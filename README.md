# Units
[![Build](https://github.com/ComposeComponents/RequestState/actions/workflows/build.yml/badge.svg)](https://github.com/ComposeComponents/RequestState/actions/workflows/build.yml)
[![Lint](https://github.com/ComposeComponents/RequestState/actions/workflows/lint.yml/badge.svg)](https://github.com/ComposeComponents/RequestState/actions/workflows/lint.yml)

A helper library for representing the state of a request.

## Installation
![Stable](https://img.shields.io/github/v/release/ComposeComponents/RequestState?label=Stable)
![Preview](https://img.shields.io/github/v/release/ComposeComponents/RequestState?label=Preview&include_prereleases)

```
implementation "cl.emilym.compose:requeststate:<latest>"
```

## Usage
```kotlin
val data = MutableLiveData<RequestState<String>>()

// The .handle extension function automatically updates the data state based on whether the 
// request failed
data.handle {
    loadString()
}

// Set a state directly
data.value = RequestState.Success("test")
```