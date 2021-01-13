const allCounties = ["Georgia", "Unknown", "Non-Georgia-Resident", "Healthcare", "Appling", "Atkinson", "Bacon", "Baker", "Baldwin", "Banks", "Barrow", "Bartow", "Ben-Hill", "Berrien", "Bibb", "Bleckley", "Brantley", "Brooks", "Bryan", "Bulloch", "Burke", "Butts", "Calhoun", "Camden", "Candler", "Carroll", "Catoosa", "Charlton", "Chatham", "Chattahoochee", "Chattooga", "Cherokee", "Clarke", "Clay", "Clayton", "Clinch", "Cobb", "Coffee", "Colquitt", "Columbia", "Cook", "Coweta", "Crawford", "Crisp", "Dade", "Dawson", "DeKalb", "Decatur", "Dodge", "Dooly", "Dougherty", "Douglas", "Early", "Echols", "Effingham", "Elbert", "Emanuel", "Evans", "Fannin", "Fayette", "Floyd", "Forsyth", "Franklin", "Fulton", "Gilmer", "Glascock", "Glynn", "Gordon", "Grady", "Greene", "Gwinnett", "Habersham", "Hall", "Hancock", "Haralson", "Harris", "Hart", "Heard", "Henry", "Houston", "Irwin", "Jackson", "Jasper", "JeffDavis", "Jefferson", "Jenkins", "Johnson", "Jones", "Lamar", "Lanier", "Laurens", "Lee", "Liberty", "Lincoln", "Long", "Lowndes", "Lumpkin", "Macon", "Madison", "Marion", "McDuffie", "McIntosh", "Meriwether", "Miller", "Mitchell", "Monroe", "Montgomery", "Morgan", "Murray", "Muscogee", "Newton", "Oconee", "Oglethorpe", "Paulding", "Peach", "Pickens", "Pierce", "Pike", "Polk", "Pulaski", "Putnam", "Quitman", "Rabun", "Randolph", "Richmond", "Rockdale", "Schley", "Screven", "Seminole", "Spalding", "Stephens", "Stewart", "Sumter", "Talbot", "Taliaferro", "Tattnall", "Taylor", "Telfair", "Terrell", "Thomas", "Tift", "Toombs", "Towns", "Treutlen", "Troup", "Turner", "Twiggs", "Union", "Upson", "Walker", "Walton", "Ware", "Warren", "Washington", "Wayne", "Webster", "Wheeler", "White", "Whitfield", "Wilcox", "Wilkes", "Wilkinson", "Worth"];
const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
const monthDates = ["2020-01-01", "2020-02-01", "2020-03-01", "2020-04-01", "2020-05-01", "2020-06-01", "2020-07-01", "2020-08-01", "2020-09-01", "2020-10-01", "2020-11-01", "2020-12-01"];

class Epicurve {

    constructor(svg, width, height, xScale, yScale, xAxis, yAxis, yScale2, yAxis2) {
        this.monthMap = [];
        for (let i = 0; i < monthNames.length; i++) {
            this.monthMap[monthDates[i]] = monthNames[i];
        }

        this.svg = svg;
        this.width = width;
        this.height = height;
        this.xScale = xScale;
        this.yScale = yScale;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.yScale2 = yScale2;
        this.yAxis2 = yAxis2;
        this.currentFocus = "mainView";

        this.globalDuration = 200;
        let selectedData = this.svg.selectAll(".prelim-region").data(["2020-08-08"]);
        let enterData = selectedData.enter();

        // draw initial region
        enterData
            .append("rect")
            .style("shape-rendering", "crispEdges")
            .attr("class", "prelim-region")
            .attr("x", d => this.xScale(d))
            .attr("y", 0)
            .attr("height", this.height)
            .attr("width", this.xScale.bandwidth())
            .style("fill", "#c6d1ff")
            .attr("opacity", "0");
    }

    getLatestDate() {
        return this.latestDate;
    }

    setLatestDate(latestDate) {
        if (!this.latestDate) {
            this.latestDate = latestDate;
        }
    }

    getEarliestDate() {
        return this.earliestDate;
    }

    setEarliestDate(earliestDate) {
        if (!this.earliestDate) {
            this.earliestDate = earliestDate;
        }
    }

    initDateSelect() {
        let constructorThis = this;
        this.lastAvailableDate = this.xScale.domain()[this.xScale.domain().length - 1];
        let constructorLastDate = this.lastAvailableDate;
        let thisCurrentFocus = this.currentFocus;
        let thisXScale = this.xScale;

        let selectedMouseDate = this.svg.selectAll(".mouse-date").data([this.lastAvailableDate]);
        selectedMouseDate.enter().append("svg:rect")
            .attr("x", -this.height - 70)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 80)
            .attr("class", "mouse-date")
            .attr("width", 70)
            .attr("height", 10)
            .attr("transform", "translate(-5,1)rotate(-90)")
            .style("fill", "#103052")
            .attr("opacity", "0");

        selectedMouseDate.enter().append("text")
            .attr("x", -this.height - 53)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 96)
            .attr("transform", "translate(9,0)rotate(-90)")
            .attr("text-anchor", "start")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("class", "mouse-date")
            .style("letter-spacing", "0px")
            .style("color", "white")
            .style("font", "10px sans-serif")
            .attr("opacity", "0")
            .text(d => d);

        let clickedMouseDate = this.svg.selectAll(".clicked-mouse-date").data([this.lastAvailableDate]);
        clickedMouseDate.enter().append("svg:rect")
            .attr("x", -this.height - 70)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 80)
            .attr("class", "clicked-mouse-date")
            .attr("width", 70)
            .attr("height", 12)
            .attr("transform", "translate(-5,1)rotate(-90)")
            .style("fill", "#103052")
            .attr("opacity", "0");

        clickedMouseDate.enter().append("text")
            .attr("x", -this.height - 53)// Note that x and y are flipped because we are rotating the text.
            .attr("y", 96)
            .attr("transform", "translate(9,0)rotate(-90)")
            .attr("text-anchor", "start")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("class", "clicked-mouse-date")
            .style("letter-spacing", "0px")
            .style("color", "white")
            .style("font", "10px sans-serif")
            .attr("opacity", "0")
            .text(d => d);

        let clickedMouseDateLine = this.svg.selectAll(".clicked-mouse-date-line").data([this.lastAvailableDate]);
        clickedMouseDateLine.enter().append("line")
            .attr("class", "clicked-mouse-date-line")
            .style("stroke", "white")
            .style("stroke-width", 1)
            .style("shape-rendering", "crispEdges")
            .attr("opacity", "0")
            .attr("x1", thisXScale(0))
            .attr("x2", thisXScale(0))
            .attr("y1", this.height + 1)
            .attr("y2", this.height + 6);

        let drag = d3.drag()
            .on('drag', function () {
                let mouse = d3.mouse(this);
                let scaledX = getDateAtMouse(mouse, thisXScale);
                constructorThis.handleMouseClick(scaledX);

                let data = d3.selectAll(".casedeltabar").filter(d => d.label === scaledX).data()[0];
                constructorThis.updateMouseOverLine(data);

                d3.select(".mouse-line")
                    .attr("d", function () {
                        let d = "M" + mouse[0] + "," + (height + 6);
                        d += " " + mouse[0] + "," + 0;
                        return d;
                    });

                d3.selectAll(".mouse-date")
                    .attr("opacity", "0")
                    .text(scaledX)
                    .attr("y", mouse[0]);
            })
            .on('start', function () {
                //handleMouseClick(getDateAtMouse(d3.mouse(this), xScale), xScale);

                let dragStartD = d3.selectAll(".casedeltabar").filter(d => d.label === d3.select("text.mouse-date").text()).data()[0];
                constructorThis.updateMouseOverLine(dragStartD);

                d3.selectAll(".mouse-date")
                    .attr("opacity", "0")
            })
            .on('end', function () {
                console.log('drag end');
            });

        this.svg.selectAll(".mouse-over-effects").data([thisCurrentFocus]).enter()
            .append("g")
            .attr("class", "mouse-over-effects");

        let mouseG = this.svg.select("g.mouse-over-effects");

        mouseG.append("path") // this is the black vertical line to follow mouse
            .attr("class", "mouse-line")
            .style("stroke", "#7ba4c9")
            .style("stroke-width", "1px")
            .style("shape-rendering", "crispEdges")
            .style("opacity", "0");

        let maxDelta = d3.max(d3.selectAll(".casedeltabar").data(), d => d.casesDelta);
        let zoomRectHeight = maxDelta ? this.yScale(0) - this.yScale(maxDelta) : this.yScale(0) - this.yScale(200);

        mouseG.append('svg:rect') // append a rect to catch mouse movements on canvas
            .attr('class', 'action-rect')
            .attr('width', width) // can't catch mouse events on a g element
            .attr('height', this.yScale(0) - zoomRectHeight)
            .attr('fill', 'none')
            .attr('pointer-events', 'all')
            .style("cursor", function () {
                return thisCurrentFocus === "mainView" ? "ew-resize" : "zoom-out"
            })
            .on('mouseout', function () { // on mouse out hide line, circles and text
                d3.select(".mouse-line")
                    .style("opacity", "0");
                d3.selectAll(".mouse-per-line circle")
                    .style("opacity", "0");
                d3.selectAll(".mouse-per-line text")
                    .style("opacity", "0");
                d3.selectAll(".mouse-date")
                    .style("opacity", "0");

            })
            .on('mouseover', function () { // on mouse in show line, circles and text
                d3.select(".mouse-line")
                    .style("opacity", "1");
                d3.selectAll(".mouse-per-line circle")
                    .style("opacity", "1");
                d3.selectAll(".mouse-per-line text")
                    .style("opacity", "1");
                d3.selectAll(".mouse-date")
                    .style("opacity", "1");
            })
            .on('mousemove', function () { // mouse moving over canvas
                let mouse = d3.mouse(this);
                let scaledX = getDateAtMouse(mouse, thisXScale);
                if (!scaledX) {
                    scaledX = thisXScale.domain()[thisXScale.domain().length - 1];
                }

                let d = d3.selectAll(".casedeltabar").filter(d => d.label === scaledX).data()[0];
                constructorThis.updateMouseOverLine(d)

                d3.select(".mouse-line")
                    .attr("d", function () {
                        let d = "M" + mouse[0] + "," + (height + 6);
                        d += " " + mouse[0] + "," + 0;
                        return d;
                    });

                d3.selectAll(".mouse-date")
                    .attr("opacity", "1")
                    .text(scaledX)
                    .attr("y", mouse[0]);

            })
            .on('click', function () {
                if (thisCurrentFocus === "caseDeltas") {
                    let targetFocus = thisCurrentFocus === "mainView" ? "caseDeltas" : "mainView";
                    let selectedDateString = d3.select(".clicked-mouse-date") ? d3.select(".clicked-mouse-date").text() : thisXScale.domain()[thisXScale.domain().length - 1];
                    handleDblClick(selectedDateString, targetFocus);
                }
            })
            .on('wheel', function () {
                let latestDate = d3.select("div#update-timestamp").text();
                let earliestDate = d3.select("div#earliest-date").text();
                let wheelDelta = d3.event.deltaY;
                let direction = wheelDelta < 0 ? '-1' : '1';
                console.log("Scrolled " + wheelDelta + " " + direction);
                d3.event.preventDefault();
                let selectedDateString = d3.select(".clicked-mouse-date") ? d3.select(".clicked-mouse-date").text() : thisXScale.domain()[thisXScale.domain().length - 1];
                console.log("Current scroll date: " + selectedDateString);
                let targetIndex = Math.floor(((thisXScale(selectedDateString) - thisXScale(thisXScale.domain()[0]) - (thisXScale.step() * direction)) / thisXScale.step()));
                let targetDate = thisXScale.domain()[targetIndex] ? thisXScale.domain()[targetIndex] : thisXScale.domain()[thisXScale.domain().length - 1];
                if (!targetDate || selectedDateString === targetDate) {
                    targetDate = applyDateOffset(selectedDateString + "T000000", -1)
                }

                let targetDateObj = new Date(targetDate + " 00:00:00");
                let earliestDateObj = convertDate(earliestDate);
                let latestDateObj = convertDate(latestDate);

                if (targetDateObj < earliestDateObj) {
                    console.log("Trying to scroll past the earliest available date!");
                    targetDate = earliestDate.substr(0, 10);
                } else if (targetDateObj > latestDateObj) {
                    console.log("Trying to scroll past the latest available date!");
                    targetDate = latestDate.substr(0, 10);
                }

                console.log("targetDate: " + targetDate);

                if (targetDate) {
                    constructorThis.handleMouseClick(targetDate);
                }
            })
            .call(drag);

        mouseG.append('svg:rect') // append a rect to catch mouse movements on canvas
            .attr('class', 'zoom-action-rect')
            .attr('width', width) // can't catch mouse events on a g element
            .attr('height', zoomRectHeight)
            .attr('y', this.yScale(0) - zoomRectHeight)
            .attr('fill', "none")
            .attr('pointer-events', 'all')
            .style("cursor", function () {
                return thisCurrentFocus === "mainView" ? "zoom-in" : "pointer"
            })
            .on("click", function () {
                let targetFocus = thisCurrentFocus === "mainView" ? "caseDeltas" : "mainView";
                let selectedDateString = d3.select(".clicked-mouse-date") ? d3.select(".clicked-mouse-date").text() : thisXScale.domain()[thisXScale.domain().length - 1];
                handleDblClick(selectedDateString, targetFocus);
            });
    }

    initDateLines(viewSwap) {
        let offScreen = 800;
        let rightMostPoint = this.xScale(this.xScale.domain()[this.xScale.domain().length - 1]);
        let transitionLength = viewSwap ? 1000 : this.globalDuration;
        let selectedDate = d3.select(".clicked-mouse-date").node() ? d3.select(".clicked-mouse-date").text() : this.xScale.domain()[this.xScale.domain().length - 1];
        let selectedDateObj = new Date(selectedDate + " 00:00:00");
        let thisXscale = this.xScale;
        let thisMonthMap = this.monthMap;
        let filteredDates = monthDates.filter(d => thisXscale(d)).map(d => {
            return {
                x: this.xScale(d) + (this.xScale.bandwidth() / 2),
                date: d,
                dateObj: new Date(d + " 00:00:00"),
                label: thisMonthMap[d]
            }
        });

        this.svg.selectAll(".month-lines").data(filteredDates, d => d.label)
            .join(enter => {
                    let enterLine = enter
                        .append("line")
                        .attr("class", "month-lines")
                        .style("stroke", "white")
                        .style("shape-rendering", "crispEdges")
                        .style("stroke-width", 1)
                        .style("stroke-dasharray", ("3, 3"))
                        .attr("x1", d => d.dateObj >= selectedDateObj ? rightMostPoint + 20 : d.x - offScreen)
                        .attr("x2", d => d.dateObj >= selectedDateObj ? rightMostPoint + 20 : d.x - offScreen)
                        .attr("y1", 0)
                        .attr("y2", this.height + 6)
                        .attr("opacity", "0.2");

                    enterLine.transition().duration(transitionLength)
                        .attr("x1", d => d.x)
                        .attr("x2", d => d.x)
                },
                update => {
                    update.transition().duration(transitionLength)
                        .attr("x1", d => d.x)
                        .attr("x2", d => d.x);
                },
                exit => {
                    exit.transition().duration(transitionLength)
                        .attr("opacity", "0.0")
                        .attr("x1", d => d.dateObj >= selectedDateObj ? rightMostPoint + 20 : d.x - offScreen)
                        .attr("x2", d => d.dateObj >= selectedDateObj ? rightMostPoint + 20 : d.x - offScreen)
                        .remove();
                });

        this.svg.selectAll(".month-text").data(filteredDates, d => d.label)
            .join(enter => {
                    let enterText = enter
                        .append("text")
                        .attr("opacity", "1")
                        .attr("class", "month-text")
                        .attr("x", -this.height - 7)
                        .attr("y", d => d.dateObj >= selectedDateObj ? rightMostPoint + 20 : d.x - offScreen)
                        .text(d => d.label)
                        .attr("text-anchor", "end")
                        .style("font", "10px sans-serif")
                        .style("alignment-baseline", "top")
                        .attr("transform", "rotate(-90)");

                    enterText.transition().duration(transitionLength)
                        .attr("y", d => this.xScale(d.date) + this.xScale.bandwidth());
                },
                update => {
                    update.transition().duration(transitionLength)
                        .attr("y", d => this.xScale(d.date) + this.xScale.bandwidth());
                },
                exit => {
                    exit.transition().duration(transitionLength)
                        .attr("opacity", "0")
                        .attr("y", d => d.dateObj >= selectedDateObj ? rightMostPoint + 20 : d.x - offScreen)
                        .remove();
                });
    }

    /**
     * Transforms the standard epicurve datapoint into a structure to be read by the hover code.
     */
    transformToHoverData(d) {
        if (!d) {
            return [];
        }

        return [{clazz: "reportedcasesbar", color: () => "#325b8d", label: d.label, y: d.reportedCases, offset: 0},
            {clazz: "caseline-circle", color: () => "#35a5ff", label: d.label, y: d.cases, offset: 0},
            {clazz: "avgline-circle", color: () => "#ff7f0e", label: d.label, y: d.movingAvg, offset: 0},
            {
                clazz: "casedeltabar",
                color: (f) => f.y < 0 ? "#777" : "#35a5ff",
                label: d.label,
                y: +d.casesDelta,
                offset: +0,
                addOnText: "Δ"
            }].filter(d => !!d.y)
    }

    /**
     * Handles rendering the hover effects.
     * @param data should be in standard epicurve format.
     */
    updateMouseOverLine(data) {
        if (!data) {
            console.log("UpdateMouseOverLine no data!!!");
            return
        }
        //console.log("UpdateMouseOverLine data: " + data.label);
        // Get around scoping issues...
        let blockYScale = this.yScale;
        let blockXScale = this.xScale;

        const minDistance = 12;
        const scaledMinDistance = blockYScale.invert(0) - blockYScale.invert(minDistance);
        const thisWidth = this.width;
        const line = d3.line()
            .x(d => d.x)
            .y(d => d.y);


        // Convert the incoming data to the format used by the hover join below.
        let rawHoverData = this.transformToHoverData(data).sort((d1, d2) => {
            if (d1.y < d2.y) {
                return -1;
            }
            if (d1.y > d2.y) {
                return 1;
            }
            return 0;
        });

        // Add offsets to avoid overlapping text
        for (let i = 1; i < rawHoverData.length; i++) {
            let prev = rawHoverData[i - 1];
            let curr = rawHoverData[i];
            let scaledDiff = curr.y - prev.y - prev.offset;

            if (scaledDiff < scaledMinDistance) {
                rawHoverData[i].offset = scaledMinDistance - scaledDiff;
            }
        }

        // We'll call this in the updateGroup. Builds the path from the hovered point to the axis on the right.
        const buildPath = d => {
            return [{x: blockXScale(d.label) + 7, y: blockYScale(d.y)},              // originating point
                {x: thisWidth, y: blockYScale(d.y)},                                 // axis point
                {x: thisWidth + 6, y: blockYScale(d.y + d.offset)}]     // right-most point with any offset applied for crowding.
        }

        // Dispatch mouseover events for the individual points. Note that we're relying on a custom data attribute selector [isHover="1"]. The event handlers
        // must set this attribute is needed.
        rawHoverData.forEach(hoverD => {
            let unHover = d3.selectAll("." + hoverD.clazz + "[isHover=\"1\"]");
            unHover.dispatch("mouseout");

            let hover = d3.selectAll("." + hoverD.clazz).filter(d => d.label === data.label);
            hover.dispatch("mouseover");
        });

        this.svg.selectAll(".hover-effects")
            .data(rawHoverData, d => d.clazz)
            .join(enter => {
                    // Create group to put the elements under
                    let enterGroup = enter.append("g").attr("class", "hover-effects");

                    // Render y text background
                    enterGroup.append("svg:rect")
                        .attr("class", d => d.clazz + "-hover")
                        .attr("x", this.width + 8)
                        .attr("y", d => blockYScale(d.y) - 6)
                        .attr("width", 45)
                        .attr("height", 12)
                        .style("fill", "#103052")
                        .attr("opacity", "1");

                    // Render line
                    enterGroup.append("path")
                        .attr("class", d => d.clazz + "-hover")
                        .style("stroke", d => d.color(d))
                        .style("shape-rendering", "crispEdges")
                        .style("stroke-width", 1)
                        .style("fill", "none")
                        .attr("d", d => line(buildPath(d)))
                        .attr("opacity", "1");

                    // Render text
                    enterGroup.append("text")
                        .attr("class", d => d.clazz + "-hover")
                        .attr("x", this.width + 17)
                        .attr("alignment-baseline", "start")
                        .attr("y", d => blockYScale(d.y) + 9)
                        .attr("dx", "-.8em")
                        .attr("dy", "-.55em")
                        .style("fill", d => d.color(d))
                        .style("font", "10px sans-serif")
                        .text(d => d.addOnText ? d.y.toLocaleString() + "" + d.addOnText : d.y.toLocaleString())
                        .attr("opacity", "1");
                },
                updateGroup => {
                    // Update positions
                    updateGroup.select("rect")
                        .attr("y", d => blockYScale(d.y + d.offset) - 6);

                    updateGroup.select("path")
                        .attr("d", d => line(buildPath(d)))
                        .style("stroke", d => d.color(d));

                    updateGroup.select("text")
                        .attr("y", d => blockYScale(d.y + d.offset) + 9)
                        .style("fill", d => d.color(d))
                        .text(d => d.addOnText ? d.y.toLocaleString() + "" + d.addOnText : d.y.toLocaleString());
                });
    }

    handleMouseClick(scaledX) {
        let targetDate = scaledX;

        if (new Date(scaledX) < earliestDate) {
            targetDate = "2020-05-13";
        }

        let event = new CustomEvent('newDateEvent', {detail: {label: targetDate}});

        dispatchEvent(event);

        this.updateClickedMouseDate(targetDate);
    }

    updateClickedMouseDate(targetDate) {
        d3.selectAll(".clicked-mouse-date")
            .attr("opacity", "1")
            .text(targetDate)
            .attr("y", this.xScale(targetDate) + (this.xScale.step() / 2));

        d3.select(".clicked-mouse-date-line")
            .attr("opacity", "1")
            .attr("x1", this.xScale(targetDate) + (this.xScale.step() / 2))
            .attr("x2", this.xScale(targetDate) + (this.xScale.step() / 2));
    }

    updateXAxis(selectedData) {
        this.xScale.domain(selectedData);
        this.xAxis = d3.axisBottom(this.xScale);

        this.svg.select("g .x.axis")
            .transition()
            .duration(this.globalDuration)
            .call(this.xAxis.tickValues([]));
        //.call(this.xAxis);
    }

    createAxisLabels(xLabel, yLabel) {
        this.svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + this.height + ")")
            .call(this.xAxis)
            .selectAll("text")
            .style("text-anchor", "end")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("transform", "rotate(-90)");

        this.svg.append("text")
            .attr("class", "x label")
            .attr("text-anchor", "end")
            .attr("x", this.width)
            .attr("y", this.height - 6)
            .text(xLabel);

        this.svg.append("g")
            .attr("class", "y axis")
            .call(this.yAxis)
            .append("text")
            .attr("y", 5)
            .attr("x", 5)
            //.attr("dy", ".71em")
            .style("text-anchor", "end")
            .text(yLabel)
            .attr("transform", "translate(5,5)rotate(-90)");
        //.attr("transform", "translate(5,5)rotate(-90)");
    }

    transitionBarChartScale(data, xCallback, yCallback, clazz) {
        this.svg.selectAll("." + clazz).data(data, d => d.label)
            .transition().duration(this.globalDuration)
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("width", this.xScale.bandwidth());
    }

    updateBarChart(data, xCallback, yCallback, keyCallback, clazz, color, highlightColor, viewSwap) {
        let transitionLength = viewSwap ? 1000 : this.globalDuration;
        let thisCurrentFocus = this.currentFocus;
        let blockThis = this;
        let thisXScale = this.xScale;
        this.svg.selectAll("." + clazz).data(data, keyCallback)
            .join(enter => {
                    let rect = enter.append("rect")
                        .style("shape-rendering", "crispEdges")
                        .attr("class", clazz)
                        .attr("width", this.xScale.bandwidth())
                        .style("cursor", function () {
                            return thisCurrentFocus === "mainView" ? "zoom-in" : "zoom-out"
                        })
                        .style("fill", d => yCallback(d) > 0 ? color : highlightColor)
                        .attr("x", d => this.xScale(xCallback(d)))
                        .attr("y", this.yScale(0))
                        .attr("height", 0)
                        .on('mouseover', function (d) {
                            d3.select(this)
                                .style("fill", "white")
                                .attr("isHover", "1");
                        })
                        .on('mouseout', function () {
                            d3.select(this)
                                .style("fill", d => yCallback(d) > 0 ? color : highlightColor)
                                .attr("isHover", "0");
                        })
                        .on("click", () => {
                            let targetFocus = thisCurrentFocus === "mainView" ? "caseDeltas" : "mainView";
                            let selectedDateString = d3.select(".clicked-mouse-date") ? d3.select(".clicked-mouse-date").text() : thisXScale.domain()[thisXScale.domain().length - 1];
                            handleDblClick(selectedDateString, targetFocus);
                        });

                    rect.transition().duration(transitionLength)
                        .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
                        .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)));
                },
                update => update
                    .transition().duration(transitionLength)
                    .attr("x", d => this.xScale(xCallback(d)))
                    .attr("y", d => yCallback(d) > 0 ? this.yScale(yCallback(d)) : this.yScale(0))
                    .attr("width", this.xScale.bandwidth())
                    .attr("height", d => Math.abs(this.yScale(yCallback(d)) - this.yScale(0)))
                    .style("fill", d => yCallback(d) > 0 ? color : highlightColor),
                exit => exit
                    .transition().duration(transitionLength)
                    .attr("y", this.yScale(0))
                    .attr("height", 0)
                    .remove());

        //this.svg.selectAll("." + clazz).raise();
    }

    updateCurveLineChart(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded, yScale) {
        let dYScale = yScale ? yScale : this.yScale;
        let line = d3.line().curve(d3.curveCardinal.tension(0.5))
            .x(d => this.getLineX(xCallback(d), isBanded))
            .y(d => this.getLineY(yCallback(d), dYScale))
            .defined(d => this.isPointDefined(yCallback, d, prelimRegionStart))

        // Handle line
        this.svg.selectAll("." + clazz).data([data], d => xCallback(d))
            .join(enter => {
                    let enterPath = enter.append("path")
                        .attr("class", clazz)
                        .attr("fill", "none")
                        .attr("stroke", color)
                        .attr("opacity", "0.5")
                        .attr("stroke-width", 2)
                        .attr("d", d3.line()
                            .x(d => this.getLineX(xCallback(d), isBanded))
                            .y(dYScale(0)));

                    enterPath.transition().duration(this.globalDuration)
                        .attr("d", line)
                },
                update => update.transition().duration(this.globalDuration)
                    .attr("d", line));

        let circleClass = clazz + '-circle';
        let isVisible = "visible" === this.svg.selectAll('.' + clazz).style("visibility");

        // Handle circles
        this.svg.selectAll('.' + circleClass).data(data)
            .join(enter => {
                    let circle = enter.append('circle')
                        .attr('class', circleClass)
                        .attr("stroke", highlightColor)
                        .attr("fill", color)
                        .attr("opacity", "0.5")
                        .attr('r', 2)
                        .style("visibility", isVisible ? "hidden" : "hidden")
                        .on('click', function () {
                            d3.select(this).attr("r", 5).attr("isHover", "1");
                        })
                        .on('mouseout', function () {
                            d3.select(this).attr("r", 1).attr("isHover", "0");
                        })
                        .attr('cx', d => this.getLineX(xCallback(d), isBanded))
                        .attr('cy', d => this.getLineY(0, dYScale));
                    circle.transition().duration(this.globalDuration)
                        .attr('cy', d => this.getLineY(yCallback(d), dYScale))
                },
                update => update
                    .attr("opacity", d => this.getLineOpacity(yCallback, d, prelimRegionStart))
                    .transition().duration(this.globalDuration)
                    .attr('cx', d => this.getLineX(xCallback(d), isBanded))
                    .attr('cy', d => this.getLineY(yCallback(d), dYScale)),
                exit => exit
                    .transition().duration(this.globalDuration)
                    .style("opacity", 0)
                    .attr('cy', d => this.getLineY(0, dYScale))
                    .remove());

        //this.svg.select(".action-rect").raise();
    }

    updateLineChartY1(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded) {
        this.updateLineChart(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded, this.yScale)
    }

    updateLineChartY2(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded) {
        this.updateLineChart(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded, this.yScale2)
    }

    updateLineChart(data, xCallback, yCallback, clazz, color, highlightColor, prelimRegionStart, isBanded, yScale) {
        let dYScale = yScale ? yScale : this.yScale;
        let line = d3.line()
            .x(d => this.getLineX(xCallback(d), isBanded))
            .y(d => this.getLineY(yCallback(d), dYScale))
            .defined(d => this.isPointDefined(yCallback, d, prelimRegionStart))

        // Handle line
        this.svg.selectAll("." + clazz).data([data], d => xCallback(d))
            .join(enter => {
                    let enterPath = enter.append("path")
                        .attr("class", clazz)
                        .attr("fill", "none")
                        .attr("stroke", color)
                        .attr("stroke-width", 1)
                        .attr("d", d3.line()
                            .x(d => this.getLineX(xCallback(d), isBanded))
                            .y(dYScale(0)));

                    // Line easein... not happy with it.
                    /*                    let node = enterPath.node();
                    let pathLength = node ? enterPath.node().getTotalLength() : 0;

                    enterPath
                        .attr("stroke-dashoffset", pathLength)
                        .attr("stroke-dasharray", pathLength)

                    enterPath.transition().ease(d3.easeSin).duration(1000)
                        .attr("stroke-dashoffset", 0)*/

                    enterPath.transition().duration(this.globalDuration)
                        .attr("d", line)
                },
                update => update.transition().duration(this.globalDuration)
                    .attr("d", line));

        let circleClass = clazz + '-circle';
        let isVisible = "visible" === this.svg.selectAll('.' + clazz).style("visibility");

        // Handle circles
        this.svg.selectAll('.' + circleClass).data(data, d => xCallback(d))
            .join(enter => {
                    let circle = enter.append('circle')
                        .attr('class', circleClass)
                        .attr("stroke", highlightColor)
                        .attr("fill", color)
                        .attr("opacity", 0)
                        .attr('r', 1)
                        .style("visibility", isVisible ? "visible" : "hidden")
                        .on('mouseover', function () {
                            d3.select(this).attr("r", 5).attr("isHover", "1");
                        })
                        .on('mouseout', function () {
                            d3.select(this).attr("r", 1).attr("isHover", "0");
                        })
                        .attr('cx', d => this.getLineX(xCallback(d), isBanded))
                        .attr('cy', d => this.getLineY(yCallback(d), dYScale));
                    circle.transition().duration(this.globalDuration)
                        .attr("opacity", d => this.getLineOpacity(yCallback, d, prelimRegionStart))
                },
                update => update
                    .attr("opacity", d => this.getLineOpacity(yCallback, d, prelimRegionStart))
                    .transition().duration(this.globalDuration)
                    .attr('cx', d => this.getLineX(xCallback(d), isBanded))
                    .attr('cy', d => this.getLineY(yCallback(d), dYScale)),
                exit => exit
                    .transition().duration(this.globalDuration)
                    .style("opacity", 0)
                    //.attr('cy', d => this.getLineY(0, dYScale))
                    .remove());

        //this.svg.select(".action-rect").raise();
    }

    updateFloatingPoints(data, xCallback, yCallback, clazz, color) {
        let filteredData = data.filter(d => !isNaN(yCallback(d)));

        filteredData.forEach(d => {
            d.x = xCallback(d);
            d.y = yCallback(d);
            d.scaledX = this.getLineX(xCallback(d), true);
            d.scaledY = this.getLineY(yCallback(d), this.yScale);
            d.color = color;
        });

        let triangle = d3.symbol()
            .size(15)
            .type(d3.symbolDiamond);

        let isVisible = true;

        if (this.svg.selectAll('.' + clazz).data().length > 0) {
            isVisible = "visible" === this.svg.selectAll('.' + clazz).style("visibility");
        }

        this.svg.selectAll("." + clazz).data(filteredData, d => xCallback(d))
            .join(enter => enter.append("path")
                    .attr("class", clazz)
                    .style("visibility", isVisible ? "visible" : "hidden")
                    .attr("d", triangle)
                    .attr("stroke", color)
                    .attr("fill", color)
                    .attr("opacity", "0.5")
                    .attr("transform", d => "translate(" + d.scaledX + "," + d.scaledY + ")")
                    .on('mouseover', function () {
                        d3.select(this)
                            .attr("opacity", "1")
                            .attr("isHover", "1")
                            .attr("d", d3.symbol().size(100).type(d3.symbolDiamond));
                    })
                    .on('mouseout', function () {
                        d3.select(this)
                            .attr("opacity", "0.5")
                            .attr("isHover", "0")
                            .attr("d", d3.symbol().size(20).type(d3.symbolDiamond));
                    }),
                update => update.transition().attr("transform", d => "translate(" + d.scaledX + "," + d.scaledY + ")"),
                exit => exit.transition().duration(this.globalDuration).style("opacity", 0).remove());

        //this.svg.select(".action-rect").raise();
    }

    updateCorrelationLine(correlationCoefficient, clazz, color) {
        let rounded = Math.round((correlationCoefficient + Number.EPSILON) * 10000) / 10000

        let selectedLine = this.svg.selectAll('.' + clazz).data([rounded]);

        let top = 80;
        let mid = top / 2;
        let right = 3500;

        selectedLine.enter()
            .append("line")
            .attr("class", clazz)
            .style("stroke", color)
            .style("stroke-width", 2)
            .attr("x1", this.xScale(0))
            .attr("y1", d => this.yScale(mid - (mid * d)))
            .attr("x2", this.xScale(right))
            .attr("y2", d => this.yScale((mid * d) + mid));

        selectedLine.merge(selectedLine)
            .transition()
            .duration(this.globalDuration)
            .attr("y1", d => this.yScale(mid - (mid * d)))
            .attr("y2", d => this.yScale((mid * d) + mid));

        let textClass = clazz + '-text';
        let selectedText = this.svg.selectAll('.' + textClass).data([rounded]);

        selectedText.enter()
            .append("text")
            .attr("class", textClass)
            .attr("x", this.xScale(right - 130))
            .attr("y", d => this.yScale((mid * d) + mid) - 10)
            .text(d => "r: " + d)
            .attr("text-anchor", "top")
            .style("alignment-baseline", "top")
            .style("fill", color)
            .style("font", "12px \"Courier New\", Courier, monospace")
        //.attr("transform", "rotate(-90)")

        selectedText
            .merge(selectedText)
            .transition()
            .duration(this.globalDuration)
            .text(d => "r: " + d)
            .attr("y", d => this.yScale((mid * d) + mid) - 10);


        selectedText.exit().remove();
    }

    getLineX(d, isBanded) {

        if (isBanded) {
            return this.xScale(d) + (this.xScale.bandwidth() / 2);
        }

        return this.xScale(d);
    }

    /**
     * Convenience function to set opacity to 0 for NaN, and 0.5 for preliminary region.
     */
    getLineOpacity(dataCallback, dataPoint, prelimRegionStart) {
        if (isNaN(dataCallback(dataPoint))) {
            return "0";
        }

        if (prelimRegionStart && this.xScale(dataPoint.label) >= this.xScale(prelimRegionStart)) {
            return "0.5";
        }

        return "1";
    }

    /**
     * Used to determine if a data point should be shown on the graph. NaN's should not be shown, and neither should points
     * after the given 'preliminary' region start date.
     *
     * @param dataCallback The callback to get the Y value
     * @param dataPoint The data point object which contains all relevant information for the given point.
     * @param prelimRegionStart The start of the preliminary region. Points after this should not be shown.
     * @returns true if the point should be shown on the graph, false if the point should not be shown.
     */
    isPointDefined(dataCallback, dataPoint, prelimRegionStart) {
        if (isNaN(dataCallback(dataPoint))) {
            return false;
        }

        if (prelimRegionStart) {
            return this.xScale(dataPoint.label) < this.xScale(prelimRegionStart);
        }

        return true;
    }

    /**
     * Convenience function to check for NaN's.
     */
    getLineY(d, yScale) {
        if (!isNaN(d)) {
            return yScale(d);
        }

        return 0;
    }

    drawMouseOverRects(xCallback, data) {
        let selectedData = this.svg.selectAll(".mouseoverclazz").data(data);
        let enterData = selectedData.enter();

        let rects = enterData.append("rect")
            .attr("class", "mouseoverclazz")
            .attr("x", d => this.xScale(xCallback(d)))
            .attr("y", 0)
            .attr("height", this.height)
            .attr("width", this.xScale.bandwidth())
            .style("fill", "#999")
            .attr("opacity", "0")
            .on("mouseover", function (d, i) {
                handleDeathMouseOver(d, i, d3.select(this), "#fff")
            })
            .on("mouseout", function (d, i) {
                handleDeathMouseOut(d, i, d3.select(this), "#999")
            });

        selectedData.exit().remove();

        return rects;
    }

    draw14DayWindow(xCallback, offsetDate, offset, viewSwap) {
        let transitionLength = viewSwap ? 1000 : 0;
        let selectedData = this.svg.selectAll(".prelim-region").data([offsetDate]);

        // update region location
        selectedData
            .merge(selectedData)
            .transition().duration(transitionLength)
            .attr("width", this.xScale.bandwidth() * (offset + 1))
            .attr("x", d => this.xScale(d))
            .attr("opacity", "0.05");

        let boundaryLineSelectedData = this.svg.selectAll(".prelim-boundary-line").data([offsetDate]);
        let boundaryLineEnterData = boundaryLineSelectedData.enter();

        // draw initial boundary line
        boundaryLineEnterData
            .append("line")
            .attr("class", "prelim-boundary-line")
            .style("stroke", "#7ba4c9")
            .style("shape-rendering", "crispEdges")
            .style("stroke-width", 1)
            .attr("x1", d => this.xScale(d))
            .attr("y1", 0)
            .attr("x2", d => this.xScale(d))
            .attr("y2", this.height);

        // update boundary line location
        boundaryLineSelectedData
            .merge(boundaryLineSelectedData)
            .transition().duration(transitionLength)
            .attr("x1", d => this.xScale(d))
            .attr("x2", d => this.xScale(d));

        // draw initial text
        let textSelectedData = this.svg.selectAll(".prelim-text").data([{date: offsetDate, label: "Preliminary Data"}]);
        let textEnterData = textSelectedData.enter();

        textEnterData
            .append("text")
            .attr("class", "prelim-text")
            .attr("x", -80)
            .attr("y", d => this.xScale(d.date) + this.xScale.bandwidth() + 5)
            .text(d => xCallback(d))
            .attr("text-anchor", "top")
            .style("alignment-baseline", "top")
            .attr("transform", "rotate(-90)")

        textSelectedData
            .merge(textSelectedData)
            .transition().duration(transitionLength)
            .attr("y", d => this.xScale(d.date) + this.xScale.bandwidth() + 5);
    }


    parseDeltaData(summaryData, caseDeltaData, histogramData, histogramCumData) {
        console.log("Parsing case delta data...");
        let tempChartData = [];
        let firstHistDate = new Date(histogramData[0]["reportDate"] + " 00:00:00");
        let filteredSummaryData = summaryData.filter(d => new Date(d.reportDate + " 00:00:00") >= firstHistDate);
        let filteredDeltaData = caseDeltaData.filter(d => convertDate(d.id) >= firstHistDate);

        for (let i = 0; i < histogramData.length; i++) {
            let currentCaseHistData = histogramData[i];
            let currentCaseHistCumData = histogramCumData[i];
            let currentCaseDeltaData = filteredDeltaData[i];
            let currentSummaryData = filteredSummaryData[i];
            let currentReportDates = Object.keys(currentCaseDeltaData).filter(d => d !== 'id' && !!currentCaseDeltaData[d]);
            let currentTimeseries = tempChartData[currentCaseDeltaData.id];

            if (!currentTimeseries) {
                currentTimeseries = [];
                tempChartData[currentCaseDeltaData.id] = currentTimeseries;
            }

            for (let j = 0; j < Math.min(100, currentReportDates.length); j++) {
                let currentGraphDate = getReverseIdxValue(currentReportDates, -j);
                currentTimeseries.push({
                    id: currentSummaryData.id,
                    rank: j,
                    label: currentGraphDate,
                    casesDelta: currentCaseDeltaData[currentGraphDate],
                    caseHist: currentCaseHistData[j],
                    caseCumHist: currentCaseHistCumData[j]
                });
            }
        }

        return tempChartData;
    }

    /**
     * Parses the provided incoming csv files into a more usable JSON document, indexed by report ID (ex: 2020-05-27T09:00:03)
     *
     * @param summaryData The summary csv
     * @param caseData The case csv
     * @param deathData The death csv
     * @param caseDeltaData The case delta csv
     * @param deathDeltaData The death delta csv
     * @param caseProjectionData The case projection csv
     * @param movingAvgData The moving averages csv
     * @param histogramData The histogram data
     * @returns [{
     *     cases: 2,
     *     deaths: 0,
     *     label: "2020-02-17"
     * },{
     *     cases: 36,
     *     deaths: 0,
     *     label: "2020-05-27"
     * }]
     */
    parseChartData(summaryData, caseData, deathData, caseDeltaData, deathDeltaData, caseProjectionData, movingAvgData) {
        let tempChartData = [];

        let reportedCasesData = []
        summaryData.forEach(function (d) {
            reportedCasesData[d.reportDate] = d.confirmedCasesVm;
        });

        let reportedDeathsData = []
        summaryData.forEach(function (d) {
            reportedDeathsData[d.reportDate] = d.deathsVm;
        });

        for (let i = 0; i < caseData.length; i++) {
            let currentCaseData = caseData[i];
            let currentDeathData = deathData[i];
            let currentCaseDeltaData = caseDeltaData[i];
            let currentDeathDeltaData = deathDeltaData[i];
            let currentMovingAvgData = movingAvgData[i];
            let currentReportDates = Object.keys(currentCaseData).filter(function (d) {
                return d !== 'id'
            }).reverse();
            let currentTimeseries = tempChartData[currentCaseData.id];
            if (!currentTimeseries) {
                currentTimeseries = [];
                tempChartData[currentCaseData.id] = currentTimeseries;
            }
            let j = 0;
            currentReportDates
                .filter(function (d) {
                    return !!currentCaseData[d];
                })
                .forEach(function (d) {
                    currentTimeseries.push({
                        label: d,
                        cases: +currentCaseData[d],
                        deaths: +currentDeathData[d],
                        casesDelta: +currentCaseDeltaData[d],
                        deathsDelta: +currentDeathDeltaData[d],
                        //casesProjection: +currentCaseProjectionData[d],
                        movingAvg: +currentMovingAvgData[d],
                        reportedCases: +reportedCasesData[d],
                        reportedDeaths: +reportedDeathsData[d],
                        rank: j
                    });
                    j++;
                });

            currentTimeseries.reverse();
        }

        return tempChartData;
    }

    parseCorrelationData(summaryData, caseData, deathData, deathOffset) {
        let tempChartData = [];

        let reportedCasesData = []
        let filteredSummaryData = summaryData.filter(d => d.reportDate >= "2020-05-11")

        filteredSummaryData.forEach(function (d) {
            reportedCasesData[d.reportDate] = d.confirmedCasesVm;
        });

        let reportedDeathsData = []
        filteredSummaryData.forEach(function (d) {
            reportedDeathsData[d.reportDate] = d.deathsVm;
        });

        let dateIndexLookup = {};
        let indexDateLookup = {};
        let allReportDates = Object.keys(deathData[deathData.length - 1]).filter(d => d !== 'id').filter(d => d >= "2020-05-11");
        for (let i = 0; i < allReportDates.length; i++) {
            let currentDate = allReportDates[i];
            dateIndexLookup[currentDate] = i;
            indexDateLookup[i] = currentDate;
        }

        for (let i = 0; i < filteredSummaryData.length; i++) {
            let currentSummaryData = filteredSummaryData[i];
            let currentCaseData = caseData[i];
            let currentDeathData = deathData[i];
            let currentReportDates = Object.keys(currentCaseData).filter(d => d !== 'id').filter(d => d >= "2020-05-11");
            let currentTimeseries = tempChartData[currentSummaryData.id];
            if (!currentTimeseries) {
                currentTimeseries = [];
                tempChartData[currentSummaryData.id] = currentTimeseries;
            }
            currentReportDates
                .filter(d => {
                    return !!currentCaseData[d];
                })
                .forEach(d => {
                    let deathOffsetDate = indexDateLookup[dateIndexLookup[d] + deathOffset];
                    if (deathOffsetDate && currentDeathData[deathOffsetDate]) {
                        currentTimeseries.push({
                            label: d,
                            cases: +currentCaseData[d],
                            deaths: +currentDeathData[deathOffsetDate],
                            casesAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, currentCaseData, d, 7),
                            deathsAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, currentDeathData, deathOffsetDate, 7),
                            reportedCases: +reportedCasesData[d] || 0,
                            reportedDeaths: +reportedDeathsData[deathOffsetDate] || 0,
                            reportedCasesAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, reportedCasesData, d, 7),
                            reportedDeathsAvg: this.calcMovingAverage(indexDateLookup, dateIndexLookup, reportedDeathsData, deathOffsetDate, 7),
                        });
                    }
                });
        }

        return tempChartData;
    }

    calcMovingAverage(indexDateLookup, dateIndexLookup, data, currentDate, window) {
        let currentDateIdx = dateIndexLookup[currentDate];

        let sum = 0;
        for (let i = 0; i < 7; i++) {
            sum += +data[indexDateLookup[currentDateIdx - i]] || 0;
        }

        return sum / window;
    }

    make_y_gridlines() {
        return d3.axisLeft(this.yScale).tickValues([0, 100, 200, 300, 400, 500, 600, 700, 800, 900])
    }

    drawAxisLines() {
        this.svg.selectAll(".grid").remove();
        this.svg.append("g")
            .attr("class", "grid")
            .call(this.make_y_gridlines()
                .tickSize(-width)
                .tickFormat("")
            );
    }

    createAxis(data, yLabel, yLabel2) {
        this.createBottomAxis(data);

        this.svg.append("g")
            .attr("class", "y axis")
            .call(this.yAxis)
            .append("text")
            .attr("y", 5)
            .attr("x", 5)
            //.attr("dy", ".71em")
            .style("text-anchor", "end")
            .text(yLabel)
            .attr("transform", "translate(5,5)rotate(-90)");

        if (this.yScale2 && this.yAxis2) {
            this.createRightSideAxis(yLabel2, false);
        }
    }

    createBottomAxis() {
        this.svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + this.height + ")")
            .call(this.xAxis.tickValues([]))
            .selectAll("text")
            .style("text-anchor", "end")
            .attr("dx", "-.8em")
            .attr("dy", "-.55em")
            .attr("transform", "rotate(-90)");
    }

    createRightSideAxis(label, leftLabel) {
        let translate = leftLabel ? "translate(-8,5)rotate(-90)" : "translate(5,5)rotate(-90)";

        this.svg.append("g")
            //.attr("transform", "translate("+(this.width-4)+",0)")
            .attr("transform", "translate(" + (this.width) + ",0)")
            .attr("class", "y axis")
            //.call(this.yAxis2 ? this.yAxis2 : this.yAxis)
            .append("text")
            .attr("class", "county-case-label")
            .attr("y", 5)
            .attr("x", 5)
            //.attr("dy", ".71em")
            .style("text-anchor", "end")
            .text(label)
            .attr("transform", translate);
    }

    createLeftSideAxis(label, axis) {
        this.svg.append("g")
            .attr("class", "y2 axis")
            .call(axis)
            .append("text")
            .attr("y", 5)
            .attr("x", 5)
            //.attr("dy", ".71em")
            .style("text-anchor", "end")
            .text(label)
            .attr("transform", "translate(5,5)rotate(-90)");
    }

    getYScaleMax(data) {
        if (deltaEnabled) {
            return 300;
        }

        return 50 + d3.max(data, d => d.cases)
    }

    getYScaleMin(data) {
        let minDelta = d3.min(data, d => d.casesDelta);
        let maxDelta = d3.max(data, d => d.casesDelta);

        if (minDelta > -10 && maxDelta < 100) {
            return -10;
        }

        if (minDelta <= -10 || maxDelta >= 100) {
            return -100
        }

        return -100;
    }

    createTooltips() {
        tip = d3.tip().attr('class', 'd3-tip').direction('e').offset([0, 5])
            .html(function (d) {
                let content = "<span style='margin-left: 2.5px;'><b>" + d.label + "</b></span><br>";
                content += `
                    <table style="margin-top: 2.5px;">
                            <tr><td>Reported Cases: </td><td style="text-align: right">` + getReportedCases(d) + `</td></tr>
                            <tr><td>Reported Deaths: </td><td style="text-align: right">` + getReportedDeaths(d) + `</td></tr>
                            <tr><td>Confirmed Cases: </td><td style="text-align: right">` + getCases(d) + `</td></tr>
                            <tr><td>Confirmed Deaths: </td><td style="text-align: right">` + getDeaths(d) + `</td></tr>
                            <tr><td>Case Delta: </td><td style="text-align: right">` + getCaseDeltas(d) + `</td></tr>
                            <tr><td>Death Delta: </td><td style="text-align: right">` + getDeathDeltas(d) + `</td></tr>
                            <tr><td>Case 7d Avg: </td><td style="text-align: right">` + getMovingAvg(d) + `</td></tr>
                    </table>
                    `;
                return content;
            });
        this.svg.call(tip);
    }

    createLineLegend(legend, clickCallback) {
        let size = 10;

        this.svg.selectAll(".legend-lines")
            .data(legend)
            .enter()
            .append("line")
            .on("click", clickCallback)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("class", "legend-lines")
            .style("stroke", d => d.color)
            .style("shape-rendering", "crispEdges")
            .style("stroke-width", 1)
            .attr("x1", d => d.x - 5)
            .attr("y1", (d, i) => this.height + 85 + (i * (size + 5)))
            .attr("x2", d => d.x - 5 + size)
            .attr("y2", (d, i) => this.height + 85 + (i * (size + 5)));

        let circleX = [];
        legend.forEach(function (d, i) {
            circleX.push({yOffset: i, x: d.x, color: d.color});
            circleX.push({yOffset: i, x: d.x + size, color: d.color});
        });

        this.svg.selectAll(".legend-circles")
            .data(circleX)
            .enter()
            .append('circle')
            .on("click", clickCallback)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr('class', "legend-circles")
            .attr('cx', d => d.x - 5)
            .attr('cy', d => this.height + 85 + (d.yOffset * (size + 5)))
            //.attr("stroke", highlightColor)
            .style("fill", d => d.color)
            .attr('r', 2);

        this.svg.selectAll("myLabels")
            .data(legend)
            .enter()
            .append("text")
            .on("click", clickCallback)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x + size + 5)
            .attr("y", (d, i) => this.height + 80 + (i * (size + 5)) + 9)
            .text(d => d.key)
            .attr("text-anchor", "left")
            .style("fill", d => d.color)
            .style("font", "11px sans-serif")
            .style("alignment-baseline", "middle")
    }

    createBoxLegend(legend) {
        let size = 8
        this.svg.selectAll("myDots")
            .data(legend)
            .enter()
            .append("rect")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x)
            .attr("y", (d, i) => getLegendHeight(this.height, size, i))
            .attr("width", size)
            .attr("height", size)
            .style("fill", d => d.color)


        this.svg.selectAll("myLabels")
            .data(legend)
            .enter()
            .append("text")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x + size + 5)
            .attr("y", (d, i) => this.height + 80 + (i * (size + 7)) + 9)
            .style("fill", d => d.textColor ? d.textColor : d.color)
            .text(d => d.key)
            .attr("text-anchor", "left")
            .style("font", "11px sans-serif")
            .style("alignment-baseline", "middle")
    }

    createShapeLegend(legend) {
        let size = 8;
        let triangle = d3.symbol()
            .type(d3.symbolDiamond)
            .size(20);

        this.svg.selectAll("myShapes")
            .data(legend)
            .enter()
            .append("path")
            //.attr("class", clazz)
            .attr("d", triangle)
            .attr("transform", (d, i) => "translate(" + (d.x + 5) + "," + (getLegendHeight(this.height, size, i) + 4) + ")")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .style("fill", d => d.color)
            .style("stroke", d => d.color);


        this.svg.selectAll("myLabels")
            .data(legend)
            .enter()
            .append("text")
            .on("click", toggleVisibility)
            .on("mouseover", function (d) {
                if (d.tooltip) {
                    d.tooltip.show();
                }
            })
            .on("mouseout", function (d) {
                if (d.tooltip) {
                    d.tooltip.hide();
                }
            })
            .attr("x", d => d.x + size + 5)
            .attr("y", (d, i) => this.height + 80 + (i * (size + 7)) + 9)
            .style("fill", d => d.textColor ? d.textColor : d.color)
            .text(d => d.key)
            .attr("text-anchor", "left")
            .style("font", "11px sans-serif")
            .style("alignment-baseline", "middle")
    }

    calculateCorrelationCoefficient(xData, yData) {
        if (xData.length !== yData.length) {
            throw "In order to calculate standard deviation, x and y must have the same number of elements!";
        }

        let NaNCount = 0;
        for (let i = 0; i < xData.length; i++) {
            if (!isNaN(xData[i]) && !isNaN(yData[i])) {
                break;
            }

            if (isNaN(xData[i]) || isNaN(yData[i])) {
                NaNCount++;
            }
        }

        //console.log("xData.length: " + xData.length + ", xData: " + xData);
        //console.log("yData.length: " + yData.length + ", yData: " + yData);
        //console.log(NaNCount + " NaN's found at the beginning of the array! yData.length: " + yData.length);
        for (let i = 0; i < NaNCount; i++) {
            xData.pop();
            yData.shift();
        }
        //console.log("After shift - xData.length: " + xData.length + ", yData.length: " + yData.length);
        //console.log("xData.length: " + xData.length + ", xData: " + xData);
        //console.log("yData.length: " + yData.length + ", yData: " + yData);

        let xMean = xData.reduce((a, b) => a + b) / xData.length;
        console.log("xMean: " + xMean);
        let yMean = yData.reduce((a, b) => a + b) / yData.length;
        console.log("yMean: " + yMean);
        let xStdev = Math.sqrt(xData.reduce(function (sq, n) {
            return sq + Math.pow(n - xMean, 2);
        }, 0) / (xData.length - 1));
        console.log("xStdev: " + xStdev);
        let yStdev = Math.sqrt(yData.reduce(function (sq, n) {
            return sq + Math.pow(n - yMean, 2);
        }, 0) / (yData.length - 1));
        console.log("yStdev: " + yStdev);
        let xStandardized = xData.map(function (d) {
            return (d - xMean) / xStdev;
        });
        //console.log("xStandardized: " + xStandardized);
        let yStandardized = yData.map(function (d) {
            return (d - yMean) / yStdev;
        });
        //console.log("yStandardized: " + yStandardized);
        let multipliedCoords = [];

        for (let i = 0; i < xStandardized.length; i++) {
            multipliedCoords.push(xStandardized[i] * yStandardized[i]);
        }

        let correlationCoefficient = multipliedCoords.reduce((a, b) => a + b) / (xData.length - 1);

        console.log("Correlation Coefficient: " + correlationCoefficient);

        return correlationCoefficient;
    }
}

d3.selection.prototype.moveToFront = function () {
    return this.each(function () {
        this.parentNode.appendChild(this);
    });
};